/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.websocket.UIPhotonConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/**
 * VisionRunner has a frame supplier, a pipeline supplier, and a result consumer; it must be closed
 * prior to the frame supplier closing
 */
@SuppressWarnings("rawtypes")
public class VisionRunner implements AutoCloseable {
    private final Logger logger;
    private final Thread visionProcessThread;
    private final FrameProvider frameSupplier;
    private final Supplier<CVPipeline> pipelineSupplier;
    private final Consumer<CVPipelineResult> pipelineResultConsumer;
    private final VisionModuleChangeSubscriber changeSubscriber;
    private final List<Runnable> runnableList = new ArrayList<Runnable>();
    private final QuirkyCamera cameraQuirks;
    private final Supplier<Integer> fpsLimitSupplier;
    private final Supplier<Boolean> enabledSupplier;

    private long loopCount;

    /**
     * VisionRunner contains a thread to run a pipeline, given a frame, and will give the result to
     * the consumer.
     *
     * @param frameSupplier
     * @param pipelineSupplier
     * @param pipelineResultConsumer
     * @param cameraQuirks
     * @param changeSubscriber The subscriber to setting changes for this VisionRunner, so it can
     *     update its settings when they change.
     * @param fpsLimitSupplier
     * @param enabledSupplier
     */
    public VisionRunner(
            FrameProvider frameSupplier,
            Supplier<CVPipeline> pipelineSupplier,
            Consumer<CVPipelineResult> pipelineResultConsumer,
            QuirkyCamera cameraQuirks,
            VisionModuleChangeSubscriber changeSubscriber,
            Supplier<Integer> fpsLimitSupplier,
            Supplier<Boolean> enabledSupplier) {
        this.frameSupplier = frameSupplier;
        this.pipelineSupplier = pipelineSupplier;
        this.pipelineResultConsumer = pipelineResultConsumer;
        this.cameraQuirks = cameraQuirks;
        this.changeSubscriber = changeSubscriber;
        this.fpsLimitSupplier = fpsLimitSupplier;
        this.enabledSupplier = enabledSupplier;

        visionProcessThread = new Thread(this::update);
        visionProcessThread.setName("VisionRunner - " + frameSupplier.getName());
        logger = new Logger(VisionRunner.class, frameSupplier.getName(), LogGroup.VisionModule);
        changeSubscriber.processSettingChanges();
    }

    public void startProcess() {
        visionProcessThread.start();
    }

    public void stopProcess() {
        try {
            System.out.println("Interrupting vision process thread");
            visionProcessThread.interrupt();
            visionProcessThread.join();
        } catch (InterruptedException e) {
            logger.error("Exception killing process thread", e);
        }
    }

    public boolean isRunning() {
        return visionProcessThread.isAlive();
    }

    public Future<Void> runSynchronously(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        synchronized (runnableList) {
            runnableList.add(
                    () -> {
                        try {
                            runnable.run();
                            future.complete(null);
                        } catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    });
        }
        return future;
    }

    public <T> Future<T> runSynchronously(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();

        synchronized (runnableList) {
            runnableList.add(
                    () -> {
                        try {
                            T result = callable.call();
                            future.complete(result);
                        } catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    });
        }

        return future;
    }

    /**
     * Waits until the next time this VisionRunner should run its pipeline, based on current FPS limit
     */
    private void waitUntilNextTick(long start) {
        int fpsLimit = fpsLimitSupplier.get();

        if (fpsLimit > 0) {
            long sleepTime = (long) (1000 / fpsLimit - (System.currentTimeMillis() - start));

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                }
            }
            return;
        } else {
            // Fall through to no limit
            return;
        }
    }

    /**
     * Try to send pipeline result to consumers. If it throws, release the result before re-throwing
     */
    private void acceptPipelineResultSafely(CVPipelineResult pipelineResult) {
        try {
            pipelineResultConsumer.accept(pipelineResult);
        } catch (Exception ex) {
            logger.error("Exception consuming pipeline result", ex);
            pipelineResult.release();
            throw ex;
        }
    }

    private void update() {
        // wait for the camera to connect
        while (!frameSupplier.isConnected() && !Thread.interrupted()) {
            // yield
            pipelineResultConsumer.accept(new CVPipelineResult(0l, 0, 0, null, new Frame()));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));

        while (!Thread.interrupted()) {
            long start = System.currentTimeMillis();
            changeSubscriber.processSettingChanges();
            synchronized (runnableList) {
                for (var runnable : runnableList) {
                    try {
                        runnable.run();
                    } catch (Exception ex) {
                        logger.error("Exception running runnable", ex);
                    }
                }
                runnableList.clear();
            }

            var pipeline = pipelineSupplier.get();

            // Tell our camera implementation here what kind of pre-processing we need it to
            // be doing
            // (pipeline-dependent). I kinda hate how much leak this has...
            // TODO would a callback object be a better fit?
            var wantedProcessType = pipeline.getThresholdType();

            frameSupplier.requestFrameThresholdType(wantedProcessType);
            var settings = pipeline.getSettings();
            if (settings instanceof AdvancedPipelineSettings advanced) {
                var hsvParams =
                        new HSVPipe.HSVParams(
                                advanced.hsvHue, advanced.hsvSaturation, advanced.hsvValue, advanced.hueInverted);
                // TODO who should deal with preventing this from happening _every single loop_?
                frameSupplier.requestHsvSettings(hsvParams);
            }
            frameSupplier.requestFrameRotation(settings.inputImageRotationMode);
            frameSupplier.requestFrameCopies(settings.inputShouldShow, settings.outputShouldShow);
            frameSupplier.requestBlockForFrames(settings.blockForFrames);

            // Grab the new camera frame
            var frame = frameSupplier.get();
            boolean frameConsumed = false;

            try {
                // Frame empty -- no point in trying to do anything more?
                if (frame.processedImage.getMat().empty() && frame.colorImage.getMat().empty()) {
                    // give up without increasing loop count
                    // Still feed with blank frames just dont run any pipelines.
                    // The original frame is released here, so mark it consumed before sending the
                    // blank result. If the consumer throws, the helper will release the blank
                    // result's owned frame and the outer finally will not double-release.
                    frame.release();
                    frameConsumed = true;
                    acceptPipelineResultSafely(new CVPipelineResult(0l, 0, 0, null, new Frame()));
                    continue;
                }

                if (!enabledSupplier.get()) {
                    // If we are skipping processing due to the camera being disabled, we still want to send a
                    // result with the new frame and settings, just with a null pipeline result
                    acceptPipelineResultSafely(new CVPipelineResult(0l, 0, 0, null, new Frame()));
                    frame.release();
                    frameConsumed = true;
                    continue;
                }

                // The pipeline will validate the supplied frame type itself and return an empty result if
                // the frame does not match its threshold requirement.

                // If we have an FPS limit, check if it's 0, in which case we skip processing and just send
                // a blank frame, otherwise we sleep until the next tick
                waitUntilNextTick(start);

                if (pipeline.getThresholdType() != FrameThresholdType.NONE
                        && frame.type != pipeline.getThresholdType()) {
                    acceptPipelineResultSafely(
                            new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame));
                    frameConsumed = true;
                    continue;
                }

                try {
                    CVPipelineResult pipelineResult = pipeline.run(frame, cameraQuirks);
                    // Frame ownership transfers to the pipeline result here.
                    frameConsumed = true;
                    acceptPipelineResultSafely(pipelineResult);
                    // If acceptPipelineResultSafely throws, the result (and its frame) were already
                    // released inside the helper.
                } catch (Exception ex) {
                    logger.error("Exception on loop " + loopCount, ex);
                    if (!frameConsumed) {
                        // Pipeline.run threw before the frame was transferred into the result.
                        frame.release();
                        frameConsumed = true;
                    }
                }
                loopCount++;
            } finally {
                // Safety net: if any branch above failed to consume or explicitly release the
                // frame, release it here to prevent a native memory leak.
                if (!frameConsumed) {
                    logger.error("Frame was not consumed; releasing it to avoid memory leak");
                    frame.release();
                }
            }
        }
    }

    @Override
    public void close() {
        if (visionProcessThread.isAlive()) {
            stopProcess();
        }
    }
}

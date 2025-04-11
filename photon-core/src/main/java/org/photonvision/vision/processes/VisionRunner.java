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
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/** VisionRunner has a frame supplier, a pipeline supplier, and a result consumer */
@SuppressWarnings("rawtypes")
public class VisionRunner {
    private final Logger logger;
    private final Thread visionProcessThread;
    private final FrameProvider frameSupplier;
    private final Supplier<CVPipeline> pipelineSupplier;
    private final Consumer<CVPipelineResult> pipelineResultConsumer;
    private final VisionModuleChangeSubscriber changeSubscriber;
    private final List<Runnable> runnableList = new ArrayList<Runnable>();
    private final QuirkyCamera cameraQuirks;

    private long loopCount;

    /**
     * VisionRunner contains a thread to run a pipeline, given a frame, and will give the result to
     * the consumer.
     *
     * @param frameSupplier The supplier of the latest frame.
     * @param pipelineSupplier The supplier of the current pipeline.
     * @param pipelineResultConsumer The consumer of the latest result.
     */
    public VisionRunner(
            FrameProvider frameSupplier,
            Supplier<CVPipeline> pipelineSupplier,
            Consumer<CVPipelineResult> pipelineResultConsumer,
            QuirkyCamera cameraQuirks,
            VisionModuleChangeSubscriber changeSubscriber) {
        this.frameSupplier = frameSupplier;
        this.pipelineSupplier = pipelineSupplier;
        this.pipelineResultConsumer = pipelineResultConsumer;
        this.cameraQuirks = cameraQuirks;
        this.changeSubscriber = changeSubscriber;

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

    private void update() {
        // wait for the camera to connect
        while (!frameSupplier.checkCameraConnected() && !Thread.interrupted()) {
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

            // Grab the new camera frame
            var frame = frameSupplier.get();

            // Frame empty -- no point in trying to do anything more?
            if (frame.processedImage.getMat().empty() && frame.colorImage.getMat().empty()) {
                // give up without increasing loop count
                // Still feed with blank frames just dont run any pipelines

                pipelineResultConsumer.accept(new CVPipelineResult(0l, 0, 0, null, new Frame()));
                continue;
            }

            // If the pipeline has changed while we are getting our frame we should scrap
            // that frame it
            // may result in incorrect frame settings like hsv values
            if (pipeline == pipelineSupplier.get()) {
                // There's no guarantee the processing type change will occur this tick, so
                // pipelines should
                // check themselves
                try {
                    var pipelineResult = pipeline.run(frame, cameraQuirks);
                    pipelineResultConsumer.accept(pipelineResult);
                } catch (Exception ex) {
                    logger.error("Exception on loop " + loopCount, ex);
                }
            }

            loopCount++;
        }
    }
}

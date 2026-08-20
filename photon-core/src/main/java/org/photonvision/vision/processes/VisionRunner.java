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
import org.opencv.core.Rect;
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
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
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
     * Side of the square tiles apriltag thresholds the decimated image in. The tiling it lands on
     * depends on where the image starts, so cropping to an origin off that grid moves every tile
     * relative to the tag and nudges the corner estimates -- and with them the reported pose -- by up
     * to a percent. Snapping the crop origin to a tile boundary keeps turning a crop on from moving
     * the numbers a tag reports.
     */
    private static final int APRILTAG_TILE_SIZE = 4;

    /**
     * Build the static crop rectangle from pipeline settings, or null if cropping is disabled or the
     * configured region is degenerate. The ranges are stored as [min, max] pixel couples.
     */
    static Rect cropRectFromSettings(AdvancedPipelineSettings settings) {
        if (!settings.staticCropEnabled) {
            return null;
        }

        // A pixel bound is never negative. Dropping the sign rather than trusting it keeps a garbage
        // bound (a value that overflowed on its way in, say) from being read as a sliver of a crop
        // one pixel from the origin.
        int xLow =
                Math.max(0, Math.min(settings.staticCropX.getFirst(), settings.staticCropX.getSecond()));
        int xHigh =
                Math.max(0, Math.max(settings.staticCropX.getFirst(), settings.staticCropX.getSecond()));
        int yLow =
                Math.max(0, Math.min(settings.staticCropY.getFirst(), settings.staticCropY.getSecond()));
        int yHigh =
                Math.max(0, Math.max(settings.staticCropY.getFirst(), settings.staticCropY.getSecond()));

        int width = xHigh - xLow;
        int height = yHigh - yLow;

        if (width <= 0 || height <= 0) {
            return null;
        }

        if (settings instanceof AprilTagPipelineSettings tagSettings) {
            // Grow the region up to the tile boundary below it rather than moving it, so the crop still
            // covers everything that was asked for.
            int tile = APRILTAG_TILE_SIZE * Math.max(1, tagSettings.decimate);
            int alignedX = (xLow / tile) * tile;
            int alignedY = (yLow / tile) * tile;

            width += xLow - alignedX;
            height += yLow - alignedY;
            xLow = alignedX;
            yLow = alignedY;
        }

        return new Rect(xLow, yLow, width, height);
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
            Rect cropRect = null;
            if (settings instanceof AdvancedPipelineSettings advanced) {
                var hsvParams =
                        new HSVPipe.HSVParams(
                                advanced.hsvHue, advanced.hsvSaturation, advanced.hsvValue, advanced.hueInverted);
                // TODO who should deal with preventing this from happening _every single loop_?
                frameSupplier.requestHsvSettings(hsvParams);

                cropRect = cropRectFromSettings(advanced);
            }
            frameSupplier.requestFrameRotation(settings.inputImageRotationMode);
            frameSupplier.requestFrameCrop(cropRect);
            frameSupplier.requestFrameCopies(settings.inputShouldShow, settings.outputShouldShow);
            frameSupplier.requestBlockForFrames(settings.blockForFrames);

            // Grab the new camera frame
            var frame = frameSupplier.get();

            // Frame empty -- no point in trying to do anything more?
            if (frame.processedImage.getMat().empty() && frame.colorImage.getMat().empty()) {
                // give up without increasing loop count
                // Still feed with blank frames just dont run any pipelines

                frame.release();
                pipelineResultConsumer.accept(new CVPipelineResult(0l, 0, 0, null, new Frame()));
            } else if (pipeline == pipelineSupplier.get()) {
                if (!enabledSupplier.get()) {
                    // If we are skipping processing due to the camera being disabled, we still want to send a
                    // result with the new frame and settings, just with a null pipeline result
                    pipelineResultConsumer.accept(new CVPipelineResult(0l, 0, 0, null, new Frame()));
                    frame.release();
                    continue;
                }

                // If the pipeline has changed while we are getting our frame we should scrap
                // that frame it may result in incorrect frame settings like hsv values

                // There's no guarantee the processing type change will occur this tick, so
                // pipelines should check themselves

                // If we have an FPS limit, check if it's 0, in which case we skip processing and just send
                // a blank frame, otherwise we sleep until the next tick
                waitUntilNextTick(start);
                try {
                    var pipelineResult = pipeline.run(frame, cameraQuirks);
                    try {
                        pipelineResultConsumer.accept(pipelineResult);
                    } catch (Exception ex) {
                        logger.error("Exception on loop " + loopCount, ex);
                        pipelineResult.release();
                    }
                } catch (Exception ex) {
                    logger.error("Pipeline exception on loop " + loopCount, ex);
                    frame.release();
                }
                loopCount++;
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

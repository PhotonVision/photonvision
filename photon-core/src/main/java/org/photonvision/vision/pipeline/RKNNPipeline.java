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

package org.photonvision.vision.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.rknn.RKNNJNI;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class RKNNPipeline extends CVPipeline<CVPipelineResult, RKNNPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private Mat processed;
    List<TrackedTarget> targetList = new ArrayList<TrackedTarget>();
    List<Long> times = new ArrayList<>();
    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    private Map<String, RKNNJNI> models = new HashMap<>();
    private Logger logger;

    public RKNNPipeline() {
        super(PROCESSING_TYPE);
        settings = new RKNNPipelineSettings();
    }

    public RKNNPipeline(RKNNPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;

        logger =
                new Logger(this.getClass(), "RKNNPipeline " + settings.pipelineNickname, LogGroup.Camera);
        unpackModelsIfNeeded();

        addModel(settings.selectedModel);
    }

    private void addModel(String name) {
        var rj = new RKNNJNI();
        rj.init(ConfigManager.getInstance().getRKNNModelsPath() + "/" + name + ".rknn");
        models.put(name, rj);
    }

    private RKNNJNI getModel(String name) {
        if (!models.containsKey(name)) {
            addModel(name);
        }
        return models.get(name);
    }

    @Override
    protected void setPipeParamsImpl() {}

    @Override
    protected CVPipelineResult process(Frame input_frame, RKNNPipelineSettings settings) {
        long sumPipeNanosElapsed = System.nanoTime();
        times.clear();
        times.add(System.nanoTime());
        if (input_frame.colorImage.getMat().empty()) {
            System.out.println("frame is empty");
            return new CVPipelineResult(0, 0, List.of(), input_frame);
        }
        times.add(System.nanoTime());
        targetList.clear();
        times.add(System.nanoTime());

        input_frame.processedImage.copyTo(input_frame.colorImage);
        times.add(System.nanoTime());
        processed = input_frame.processedImage.getMat();

        times.add(System.nanoTime());
        var results =
                getModel(settings.selectedModel)
                        .detectAndDisplay(input_frame.colorImage.getMat().getNativeObjAddr());
        times.add(System.nanoTime());
        for (int i = 0; results != null && i < results.count; i++) {
            var detection = results.results[i];
            if (detection.conf < settings.confidenceThreshold) continue;

            var box = detection.box;
            targetList.add(
                    new TrackedTarget(
                            new Rect2d(box.left, box.top, box.right - box.left, box.bottom - box.top),
                            detection.cls,
                            detection.conf,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, this.frameStaticProperties)));
            if (settings.outputShouldShow) {
                Imgproc.rectangle(
                        processed,
                        new Point(box.left, box.top),
                        new Point(box.right, box.bottom),
                        new Scalar(0, 0, 255),
                        2);

                var name = String.format("%s (%f)", Short.toString(detection.cls), detection.conf);

                Imgproc.putText(
                        processed,
                        name,
                        new Point(box.left, box.top + 12),
                        0,
                        0.6,
                        ColorHelper.colorToScalar(java.awt.Color.white),
                        2);
            }
        }
        times.add(System.nanoTime());

        Size size = null;
        switch (settings.streamingFrameDivisor) {
            case NONE:
                break;
            case HALF:
                size = new Size(processed.width() / 2, processed.height() / 2);
                break;
            case QUARTER:
                size = new Size(processed.width() / 4, processed.height() / 4);
                break;
            case SIXTH:
                size = new Size(processed.width() / 6, processed.height() / 6);
                break;
            default:
                break;
        }
        if (size != null) {
            Imgproc.resize(processed, processed, size);
            Imgproc.resize(input_frame.colorImage.getMat(), input_frame.colorImage.getMat(), size);
        }
        times.add(System.nanoTime());

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        // print times
        for (int i = 0; i < times.size() - 1; i++) {
            // System.out.print((times.get(i + 1) - times.get(i)) / 1000000.0 + " ");
        }
        // System.out.print((System.nanoTime() - sumPipeNanosElapsed) / 1000000.0 + " ");
        // System.out.println((times.get(times.size() - 1) - times.get(0)) / 1000000.0);

        return new CVPipelineResult(
                System.nanoTime() - sumPipeNanosElapsed, fps, targetList, input_frame);
    }

    void unpackModelsIfNeeded() {
        var modelsPath = ConfigManager.getInstance().getRKNNModelsPath();
        if (!modelsPath.toFile().exists()) {
            logger.info("Unpacking RKNN models...");
            var stream = getClass().getResourceAsStream("/models.zip");
            if (stream == null) {
                logger.error("Failed to find models.zip in jar");
                return;
            }
            try {
                Files.createDirectories(modelsPath);
                var zip = new ZipInputStream(stream);
                var entry = zip.getNextEntry();
                while (entry != null) {
                    var filePath = modelsPath.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.copy(zip, filePath);
                    }
                    entry = zip.getNextEntry();
                }
                zip.close();
            } catch (IOException e) {
                logger.error("Failed to unpack models.zip");
                e.printStackTrace();
            }
        }
    }
}

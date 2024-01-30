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
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import org.opencv.core.*;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TargetOrientation;
import org.photonvision.vision.target.TrackedTarget;

public class RKNNPipeline extends CVPipeline<CVPipelineResult, RKNNPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final RKNNPipe rknnPipe = new RKNNPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final FilterObjectDetectionsPipe filterContoursPipe = new FilterObjectDetectionsPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

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
    }

    @Override
    protected void setPipeParamsImpl() {
        var params = new RKNNPipe.RKNNPipeParams();
        params.confidenceThreshold = settings.confidenceThreshold;
        params.modelPath = settings.selectedModel;
        rknnPipe.setParams(params);

        DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        SortContoursPipe.SortContoursParams sortContoursParams =
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets ? 999 : 1,
                        frameStaticProperties);
        sortContoursPipe.setParams(sortContoursParams);

        var filterContoursParams =
                new FilterObjectDetectionsPipe.FilterContoursParams(
                        settings.contourArea,
                        settings.contourRatio,
                        frameStaticProperties,
                        settings.contourTargetOrientation == TargetOrientation.Landscape);
        filterContoursPipe.setParams(filterContoursParams);

        Collect2dTargetsPipe.Collect2dTargetsParams collect2dTargetsParams =
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties);
        collect2dTargetsPipe.setParams(collect2dTargetsParams);
    }

    @Override
    protected CVPipelineResult process(Frame input_frame, RKNNPipelineSettings settings) {
        long timeStarted = System.nanoTime();
        long sumPipeNanosElapsed = 0;

        input_frame.processedImage.copyTo(input_frame.colorImage);
        var processed = input_frame.processedImage.getMat();

        CVPipeResult<List<NeuralNetworkPipeResult>> rknnResult = rknnPipe.run(input_frame.colorImage);
        sumPipeNanosElapsed += rknnResult.nanosElapsed;

        if (rknnResult.output.size() == 0)
            return new CVPipelineResult(0, calculateFPSPipe.run(null).output, List.of(), input_frame);

        var filterContoursResult = filterContoursPipe.run(rknnResult.output);
        sumPipeNanosElapsed += filterContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(
                        filterContoursResult.output.stream()
                                .map(shape -> new PotentialTarget(shape))
                                .collect(Collectors.toList()));
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        // Size size = null;
        // switch (settings.streamingFrameDivisor) {
        //     case NONE:
        //         break;
        //     case HALF:
        //         size = new Size(processed.width() / 2, processed.height() / 2);
        //         break;
        //     case QUARTER:
        //         size = new Size(processed.width() / 4, processed.height() / 4);
        //         break;
        //     case SIXTH:
        //         size = new Size(processed.width() / 6, processed.height() / 6);
        //         break;
        //     default:
        //         break;
        // }

        // if (settings.outputShouldShow) {
        //     // for (var target : rknnResult.output) {
        //     // Imgproc.rectangle(
        //     // processed,
        //     // new Point(target.box.x, target.box.y),
        //     // new Point(target.box.x + target.box.width, target.box.y + target.box.height),
        //     // isIn(filterContoursResult.output, target) ? new Scalar(0, 255, 0) : new
        //     // Scalar(0, 0, 255),
        //     // 2 * processed.width() / 640);
        //     // var label = String.format("%s (%f)", target.classIdx, target.confidence);
        //     // Imgproc.putText(
        //     // processed,
        //     // label,
        //     // new Point(target.box.x, target.box.y + 12),
        //     // 0,
        //     // 0.6 * processed.width() / 640,
        //     // ColorHelper.colorToScalar(java.awt.Color.white),
        //     // 2 * processed.width() / 640);
        //     // }
        //     // Imgproc.resize(processed, processed, size);
        // }
        // // Imgproc.resize(input_frame.colorImage.getMat(),
        // // input_frame.colorImage.getMat(), size);

        return new CVPipelineResult(
                (System.nanoTime() - timeStarted) * 1e-6,
                calculateFPSPipe.run(null).output,
                collect2dTargetsResult.output,
                input_frame);
    }

    boolean isIn(List<NeuralNetworkPipeResult> list, NeuralNetworkPipeResult target) {
        for (var item : list)
            if (item.box.equals(target.box)
                    && item.classIdx == target.classIdx
                    && item.confidence == target.confidence) return true;
        return false;
    }

    boolean shouldUnpackModels() throws IOException {
        var unpacked = ConfigManager.getInstance().getRKNNModelsPath().resolve("unpacked").toFile();
        if (!unpacked.exists()) {
            logger.info("RKNN models not unpacked");
            return true;
        }
        var lines = Files.readAllLines(unpacked.toPath());
        if (lines.size() == 0 || !PhotonVersion.versionMatches(lines.get(0))) {
            logger.info("RKNN models version mismatch");
            return true;
        }
        return false;
    }

    void unpackModelsIfNeeded() {
        var modelsPath = ConfigManager.getInstance().getRKNNModelsPath();
        try {
            if (shouldUnpackModels()) {
                logger.info("Unpacking RKNN models...");
                var stream = getClass().getResourceAsStream("/models.zip");
                if (stream == null) {
                    logger.error("Failed to find models.zip in jar");
                    return;
                }
                Files.createDirectories(modelsPath);
                var zip = new ZipInputStream(stream);
                var entry = zip.getNextEntry();
                while (entry != null) {
                    var filePath = modelsPath.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                        Files.copy(zip, filePath);
                    }
                    entry = zip.getNextEntry();
                }
                zip.close();
                Files.write(modelsPath.resolve("unpacked"), PhotonVersion.versionString.getBytes());
            }
        } catch (IOException e) {
            logger.error("Failed to unpack models.zip");
            e.printStackTrace();
        }
    }
}

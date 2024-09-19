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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.NullModel;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.ObjectDetectionPipe.ObjectDetectionPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TargetOrientation;
import org.photonvision.vision.target.TrackedTarget;

public class ObjectDetectionPipeline
        extends CVPipeline<CVPipelineResult, ObjectDetectionPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final ObjectDetectionPipe objectDetectorPipe = new ObjectDetectionPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final FilterObjectDetectionsPipe filterContoursPipe = new FilterObjectDetectionsPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public ObjectDetectionPipeline() {
        super(PROCESSING_TYPE);
        settings = new ObjectDetectionPipelineSettings();
    }

    public ObjectDetectionPipeline(ObjectDetectionPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        var params = new ObjectDetectionPipeParams();
        params.confidence = settings.confidence;
        params.nms = settings.nms;
        Optional<Model> selectedModel =
                NeuralNetworkModelManager.getInstance().getModel(settings.model);

        // If the desired model couldn't be found, log an error and try to use the default model
        if (selectedModel.isEmpty()) {
            selectedModel = NeuralNetworkModelManager.getInstance().getDefaultModel();
        }

        // If the model remains empty, use the NullModel
        if (selectedModel.isEmpty()) {
            selectedModel = Optional.of(NullModel.getInstance());
        }

        params.model = selectedModel.get();

        objectDetectorPipe.setParams(params);

        DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        SortContoursPipe.SortContoursParams sortContoursParams =
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets ? MAX_MULTI_TARGET_RESULTS : 1,
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
    protected CVPipelineResult process(Frame frame, ObjectDetectionPipelineSettings settings) {
        long sumPipeNanosElapsed = 0;

        // ***************** change based on backend ***********************

        CVPipeResult<List<NeuralNetworkPipeResult>> rknnResult =
                objectDetectorPipe.run(frame.colorImage);
        sumPipeNanosElapsed += rknnResult.nanosElapsed;

        var names = objectDetectorPipe.getClassNames();

        frame.colorImage.getMat().copyTo(frame.processedImage.getMat());

        // ***************** change based on backend ***********************

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

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                frame.sequenceID, sumPipeNanosElapsed, fps, collect2dTargetsResult.output, frame, names);
    }

    @Override
    public void release() {
        objectDetectorPipe.release();
        super.release();
    }
}

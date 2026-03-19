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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opencv.core.Point;
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
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();

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
        Optional<Model> selectedModel =
                settings.model != null
                        ? NeuralNetworkModelManager.getInstance()
                                .getModel(settings.model.modelPath().toString())
                        : Optional.empty();

        // If the desired model couldn't be found, log an error and try to use the default model
        if (selectedModel.isEmpty()) {
            selectedModel = NeuralNetworkModelManager.getInstance().getDefaultModel();
        }

        // If the model remains empty, use the NullModel
        if (selectedModel.isEmpty()) {
            selectedModel = Optional.of(NullModel.getInstance());
        }
        objectDetectorPipe.setParams(
                new ObjectDetectionPipeParams(settings.confidence, settings.nms, selectedModel.get()));

        DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        sortContoursPipe.setParams(
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode, settings.outputMaximumTargets, frameStaticProperties));

        filterContoursPipe.setParams(
                new FilterObjectDetectionsPipe.FilterContoursParams(
                        settings.contourArea,
                        settings.contourRatio,
                        frameStaticProperties,
                        settings.contourTargetOrientation == TargetOrientation.Landscape));

        collect2dTargetsPipe.setParams(
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties));

        solvePNPPipe.setParams(
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration, settings.targetModel));
    }

    @Override
    protected CVPipelineResult process(Frame frame, ObjectDetectionPipelineSettings settings) {
        long sumPipeNanosElapsed = 0;

        CVPipeResult<List<NeuralNetworkPipeResult>> neuralNetworkResult =
                objectDetectorPipe.run(frame.colorImage);
        sumPipeNanosElapsed += neuralNetworkResult.nanosElapsed;

        var names = objectDetectorPipe.getClassNames();

        frame.colorImage.getMat().copyTo(frame.processedImage.getMat());

        var filterContoursResult = filterContoursPipe.run(neuralNetworkResult.output);
        sumPipeNanosElapsed += filterContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(
                        filterContoursResult.output.stream().map(shape -> new PotentialTarget(shape)).toList());
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        // 3d stuff
        if (settings.solvePNPEnabled) {
            var rectPoints = new Point[4];
            collect2dTargetsResult.output.forEach(
                    target -> {
                        target.getMinAreaRect().points(rectPoints);
                        if (settings.targetModel.isSpherical()) {
                            // For symmetric (spherical) targets such as balls: the model presents
                            // the same circular silhouette from every angle, so any mapping of the
                            // 4 OBB corners to the 4 equatorial model points yields the same pose.
                            // Just hand the corners through as-is.
                            target.setTargetCorners(
                                    Arrays.asList(rectPoints[0], rectPoints[1], rectPoints[2], rectPoints[3]));
                        } else {
                            // For non-symmetric targets the OBB side ratio indicates which face of
                            // the 3D object is visible.  RotatedRect.points() returns corners in
                            // order (bottom-left, top-left, top-right, bottom-right); reorder to
                            // (bottom-left, bottom-right, top-right, top-left) to match the
                            // solvePNP model convention expected by CornerDetectionPipe.
                            // TODO: when TargetModel gains multi-face support for 3D non-symmetric
                            // objects, select the appropriate face's 3D corners based on the ratio
                            // of the OBB's width to its height before calling solvePNP.
                            target.setTargetCorners(
                                    Arrays.asList(rectPoints[0], rectPoints[3], rectPoints[2], rectPoints[1]));
                        }
                    });

            var solvePNPResult = solvePNPPipe.run(collect2dTargetsResult.output);
            sumPipeNanosElapsed += solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.output;
        } else {
            targetList = collect2dTargetsResult.output;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                frame.sequenceID, sumPipeNanosElapsed, fps, targetList, frame, names);
    }

    @Override
    public void release() {
        objectDetectorPipe.release();
        super.release();
    }
}

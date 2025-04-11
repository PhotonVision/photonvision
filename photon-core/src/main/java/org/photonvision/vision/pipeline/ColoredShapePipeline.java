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

import edu.wpi.first.math.Pair;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Point;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;

public class ColoredShapePipeline
        extends CVPipeline<CVPipelineResult, ColoredShapePipelineSettings> {
    private final SpeckleRejectPipe speckleRejectPipe = new SpeckleRejectPipe();
    private final FindContoursPipe findContoursPipe = new FindContoursPipe();
    private final FindPolygonPipe findPolygonPipe = new FindPolygonPipe();
    private final FindCirclesPipe findCirclesPipe = new FindCirclesPipe();
    private final FilterShapesPipe filterShapesPipe = new FilterShapesPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final CornerDetectionPipe cornerDetectionPipe = new CornerDetectionPipe();
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final Draw2dTargetsPipe draw2DTargetsPipe = new Draw2dTargetsPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private final Point[] rectPoints = new Point[4];

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.HSV;

    public ColoredShapePipeline() {
        super(PROCESSING_TYPE);
        settings = new ColoredShapePipelineSettings();
    }

    public ColoredShapePipeline(ColoredShapePipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        speckleRejectPipe.setParams(
                new SpeckleRejectPipe.SpeckleRejectParams(settings.contourSpecklePercentage));

        findContoursPipe.setParams(new FindContoursPipe.FindContoursParams());

        findPolygonPipe.setParams(
                new FindPolygonPipe.FindPolygonPipeParams(settings.accuracyPercentage));

        findCirclesPipe.setParams(
                new FindCirclesPipe.FindCirclePipeParams(
                        settings.circleDetectThreshold,
                        settings.contourRadius.getFirst(),
                        settings.minDist,
                        settings.contourRadius.getSecond(),
                        settings.maxCannyThresh,
                        settings.circleAccuracy,
                        Math.hypot(frameStaticProperties.imageWidth, frameStaticProperties.imageHeight)));

        filterShapesPipe.setParams(
                new FilterShapesPipe.FilterShapesPipeParams(
                        settings.contourShape,
                        settings.contourArea.getFirst(),
                        settings.contourArea.getSecond(),
                        settings.contourPerimeter.getFirst(),
                        settings.contourPerimeter.getSecond(),
                        frameStaticProperties));

        sortContoursPipe.setParams(
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets
                                ? MAX_MULTI_TARGET_RESULTS // TODO don't hardcode?
                                : 1,
                        frameStaticProperties));

        collect2dTargetsPipe.setParams(
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties));

        cornerDetectionPipe.setParams(
                new CornerDetectionPipe.CornerDetectionPipeParameters(
                        settings.cornerDetectionStrategy,
                        settings.cornerDetectionUseConvexHulls,
                        settings.cornerDetectionExactSideCount,
                        settings.cornerDetectionSideCount,
                        settings.cornerDetectionAccuracyPercentage));

        solvePNPPipe.setParams(
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration, settings.targetModel));

        Draw2dTargetsPipe.Draw2dTargetsParams draw2DTargetsParams =
                new Draw2dTargetsPipe.Draw2dTargetsParams(
                        settings.outputShouldDraw,
                        settings.outputShowMultipleTargets,
                        settings.streamingFrameDivisor);
        draw2DTargetsParams.showShape = true;
        draw2DTargetsParams.showMaximumBox = false;
        draw2DTargetsParams.showRotatedBox = false;
        draw2DTargetsPipe.setParams(draw2DTargetsParams);

        draw2dCrosshairPipe.setParams(
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.outputShouldDraw,
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        frameStaticProperties,
                        settings.streamingFrameDivisor,
                        settings.inputImageRotationMode));

        draw3dTargetsPipe.setParams(
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.outputShouldDraw,
                        frameStaticProperties.cameraCalibration,
                        settings.targetModel,
                        settings.streamingFrameDivisor));
    }

    @Override
    protected CVPipelineResult process(Frame frame, ColoredShapePipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        CVPipeResult<List<Contour>> findContoursResult =
                findContoursPipe.run(frame.processedImage.getMat());
        sumPipeNanosElapsed += findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.run(findContoursResult.output);
        sumPipeNanosElapsed += speckleRejectResult.nanosElapsed;

        List<CVShape> shapes = null;
        if (settings.contourShape == ContourShape.Circle) {
            CVPipeResult<List<CVShape>> findCirclesResult =
                    findCirclesPipe.run(Pair.of(frame.processedImage.getMat(), speckleRejectResult.output));
            sumPipeNanosElapsed += findCirclesResult.nanosElapsed;
            shapes = findCirclesResult.output;
        } else {
            CVPipeResult<List<CVShape>> findPolygonsResult =
                    findPolygonPipe.run(speckleRejectResult.output);
            sumPipeNanosElapsed += findPolygonsResult.nanosElapsed;
            shapes = findPolygonsResult.output;
        }

        CVPipeResult<List<CVShape>> filterShapeResult = filterShapesPipe.run(shapes);
        sumPipeNanosElapsed += filterShapeResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(
                        filterShapeResult.output.stream()
                                .map(shape -> new PotentialTarget(shape.getContour(), shape))
                                .toList());
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        if (settings.solvePNPEnabled && settings.contourShape == ContourShape.Circle) {
            var cornerDetectionResult = cornerDetectionPipe.run(collect2dTargetsResult.output);
            collect2dTargetsResult.output.forEach(
                    shape -> {
                        shape.getMinAreaRect().points(rectPoints);
                        shape.setTargetCorners(Arrays.asList(rectPoints));
                    });
            sumPipeNanosElapsed += cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.run(cornerDetectionResult.output);
            sumPipeNanosElapsed += solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.output;
        } else {
            targetList = collect2dTargetsResult.output;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(frame.sequenceID, sumPipeNanosElapsed, fps, targetList, frame);
    }
}

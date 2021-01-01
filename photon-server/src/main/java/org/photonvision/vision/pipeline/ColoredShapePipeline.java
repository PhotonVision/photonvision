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
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.*;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;

@SuppressWarnings({"DuplicatedCode"})
public class ColoredShapePipeline
        extends CVPipeline<CVPipelineResult, ColoredShapePipelineSettings> {

    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final ErodeDilatePipe erodeDilatePipe = new ErodeDilatePipe();
    private final HSVPipe hsvPipe = new HSVPipe();
    private final OutputMatPipe outputMatPipe = new OutputMatPipe();
    private final SpeckleRejectPipe speckleRejectPipe = new SpeckleRejectPipe();
    private final FindContoursPipe findContoursPipe = new FindContoursPipe();
    private final FindPolygonPipe findPolygonPipe = new FindPolygonPipe();
    private final FindCirclesPipe findCirclesPipe = new FindCirclesPipe();
    private final FilterShapesPipe filterShapesPipe = new FilterShapesPipe();
    private final GroupContoursPipe groupContoursPipe = new GroupContoursPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final CornerDetectionPipe cornerDetectionPipe = new CornerDetectionPipe();
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final Draw2dTargetsPipe draw2DTargetsPipe = new Draw2dTargetsPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private final Mat rawInputMat = new Mat();
    private final Point[] rectPoints = new Point[4];

    public ColoredShapePipeline() {
        settings = new ColoredShapePipelineSettings();
    }

    public ColoredShapePipeline(ColoredShapePipelineSettings settings) {
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

        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        ErodeDilatePipe.ErodeDilateParams erodeDilateParams =
                new ErodeDilatePipe.ErodeDilateParams(settings.erode, settings.dilate, 5);
        // TODO: add kernel size to pipeline settings
        erodeDilatePipe.setParams(erodeDilateParams);

        HSVPipe.HSVParams hsvParams =
                new HSVPipe.HSVParams(settings.hsvHue, settings.hsvSaturation, settings.hsvValue);
        hsvPipe.setParams(hsvParams);

        OutputMatPipe.OutputMatParams outputMatParams = new OutputMatPipe.OutputMatParams();
        outputMatPipe.setParams(outputMatParams);

        SpeckleRejectPipe.SpeckleRejectParams speckleRejectParams =
                new SpeckleRejectPipe.SpeckleRejectParams(settings.contourSpecklePercentage);
        speckleRejectPipe.setParams(speckleRejectParams);

        FindContoursPipe.FindContoursParams findContoursParams =
                new FindContoursPipe.FindContoursParams();
        findContoursPipe.setParams(findContoursParams);

        FindPolygonPipe.FindPolygonPipeParams findPolygonPipeParams =
                new FindPolygonPipe.FindPolygonPipeParams(settings.accuracyPercentage);
        findPolygonPipe.setParams(findPolygonPipeParams);

        FindCirclesPipe.FindCirclePipeParams findCirclePipeParams =
                new FindCirclesPipe.FindCirclePipeParams(
                        settings.allowableThreshold,
                        settings.minRadius,
                        settings.minDist,
                        settings.maxRadius,
                        settings.maxCannyThresh,
                        settings.accuracy);
        findCirclesPipe.setParams(findCirclePipeParams);

        FilterShapesPipe.FilterShapesPipeParams filterShapesPipeParams =
                new FilterShapesPipe.FilterShapesPipeParams(
                        settings.desiredShape,
                        settings.minArea,
                        settings.maxArea,
                        settings.minPeri,
                        settings.maxPeri);
        filterShapesPipe.setParams(filterShapesPipeParams);

        GroupContoursPipe.GroupContoursParams groupContoursParams =
                new GroupContoursPipe.GroupContoursParams(
                        settings.contourGroupingMode, settings.contourIntersection);
        groupContoursPipe.setParams(groupContoursParams);

        SortContoursPipe.SortContoursParams sortContoursParams =
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets ? 5 : 1,
                        frameStaticProperties); // TODO don't hardcode?
        sortContoursPipe.setParams(sortContoursParams);

        Collect2dTargetsPipe.Collect2dTargetsParams collect2dTargetsParams =
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties);
        collect2dTargetsPipe.setParams(collect2dTargetsParams);

        var params =
                new CornerDetectionPipe.CornerDetectionPipeParameters(
                        settings.cornerDetectionStrategy,
                        settings.cornerDetectionUseConvexHulls,
                        settings.cornerDetectionExactSideCount,
                        settings.cornerDetectionSideCount,
                        settings.cornerDetectionAccuracyPercentage);
        cornerDetectionPipe.setParams(params);

        var solvePNPParams =
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration,
                        frameStaticProperties.cameraPitch,
                        settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);

        Draw2dTargetsPipe.Draw2dTargetsParams draw2DTargetsParams =
                new Draw2dTargetsPipe.Draw2dTargetsParams(
                        settings.outputShouldDraw,
                        settings.outputShowMultipleTargets,
                        settings.streamingFrameDivisor);
        draw2DTargetsParams.showShape = true;
        draw2DTargetsParams.showMaximumBox = false;
        draw2DTargetsParams.showRotatedBox = false;
        draw2DTargetsPipe.setParams(draw2DTargetsParams);

        Draw2dCrosshairPipe.Draw2dCrosshairParams draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.outputShouldDraw,
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        frameStaticProperties,
                        settings.streamingFrameDivisor);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        var draw3dTargetsParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.outputShouldDraw,
                        frameStaticProperties.cameraCalibration,
                        settings.targetModel,
                        settings.streamingFrameDivisor);
        draw3dTargetsPipe.setParams(draw3dTargetsParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, ColoredShapePipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        var rotateImageResult = rotateImagePipe.run(frame.image.getMat());
        sumPipeNanosElapsed += rotateImageResult.nanosElapsed;

        rawInputMat.release();
        frame.image.getMat().copyTo(rawInputMat);

        var erodeDilateResult = erodeDilatePipe.run(rawInputMat);
        sumPipeNanosElapsed += erodeDilateResult.nanosElapsed;

        CVPipeResult<Mat> hsvPipeResult = hsvPipe.run(rawInputMat);
        sumPipeNanosElapsed += hsvPipeResult.nanosElapsed;

        CVPipeResult<List<Contour>> findContoursResult = findContoursPipe.run(hsvPipeResult.output);
        sumPipeNanosElapsed += findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.run(findContoursResult.output);
        sumPipeNanosElapsed += speckleRejectResult.nanosElapsed;

        List<CVShape> shapes;
        if (settings.desiredShape == ContourShape.Circle) {
            CVPipeResult<List<CVShape>> findCirclesResult =
                    findCirclesPipe.run(Pair.of(hsvPipeResult.output, speckleRejectResult.output));
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

        CVPipeResult<List<PotentialTarget>> groupContoursResult =
                groupContoursPipe.run(
                        filterShapeResult.output.stream()
                                .map(CVShape::getContour)
                                .collect(Collectors.toList()));
        sumPipeNanosElapsed += groupContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(groupContoursResult.output);
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        if (settings.solvePNPEnabled && settings.desiredShape == ContourShape.Circle) {
            var cornerDetectionResult = cornerDetectionPipe.run(collect2dTargetsResult.output);
            collect2dTargetsResult.output.forEach(
                    shape -> {
                        shape.getMinAreaRect().points(rectPoints);
                        shape.setCorners(Arrays.asList(rectPoints));
                    });
            sumPipeNanosElapsed += cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.run(cornerDetectionResult.output);
            sumPipeNanosElapsed += solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.output;
        } else {
            targetList = collect2dTargetsResult.output;
        }

        // Draw 2D Crosshair on input and output
        var draw2dCrosshairResultOnInput = draw2dCrosshairPipe.run(Pair.of(rawInputMat, targetList));
        sumPipeNanosElapsed += draw2dCrosshairResultOnInput.nanosElapsed;

        var draw2dCrosshairResultOnOutput =
                draw2dCrosshairPipe.run(Pair.of(hsvPipeResult.output, targetList));
        sumPipeNanosElapsed += draw2dCrosshairResultOnOutput.nanosElapsed;

        // Draw 2D contours on input and output
        var draw2dContoursResultOnInput =
                draw2DTargetsPipe.run(Pair.of(rawInputMat, collect2dTargetsResult.output));
        sumPipeNanosElapsed += draw2dContoursResultOnInput.nanosElapsed;

        var draw2dContoursResultOnOutput =
                draw2DTargetsPipe.run(Pair.of(hsvPipeResult.output, collect2dTargetsResult.output));
        sumPipeNanosElapsed += draw2dContoursResultOnOutput.nanosElapsed;

        if (settings.solvePNPEnabled && settings.desiredShape == ContourShape.Circle) {
            var drawOnInputResult =
                    draw3dTargetsPipe.run(Pair.of(rawInputMat, collect2dTargetsResult.output));
            sumPipeNanosElapsed += drawOnInputResult.nanosElapsed;

            var drawOnOutputResult =
                    draw3dTargetsPipe.run(Pair.of(hsvPipeResult.output, collect2dTargetsResult.output));
            sumPipeNanosElapsed += drawOnOutputResult.nanosElapsed;
        }

        // Convert single-channel HSV output mat to 3-channel BGR in preparation for streaming
        var outputMatPipeResult = outputMatPipe.run(hsvPipeResult.output);
        sumPipeNanosElapsed += outputMatPipeResult.nanosElapsed;

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                fps,
                targetList,
                new Frame(new CVMat(hsvPipeResult.output), frame.frameStaticProperties),
                new Frame(new CVMat(rawInputMat), frame.frameStaticProperties));
    }
}

package com.chameleonvision.common.vision.pipeline;

import static com.chameleonvision.common.vision.pipe.impl.FilterShapesPipe.*;
import static com.chameleonvision.common.vision.pipe.impl.FindPolygonPipe.*;

import com.chameleonvision.common.util.math.MathUtils;
import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;
import com.chameleonvision.common.vision.opencv.*;
import com.chameleonvision.common.vision.pipe.CVPipeResult;
import com.chameleonvision.common.vision.pipe.impl.*;
import com.chameleonvision.common.vision.target.PotentialTarget;
import com.chameleonvision.common.vision.target.TrackedTarget;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;

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
    private final Draw2dContoursPipe draw2dContoursPipe = new Draw2dContoursPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();

    private final Mat rawInputMat = new Mat();
    private final DualMat outputMats = new DualMat();
    private List<CVShape> shapes;
    private CVPipeResult<Mat> result;
    private CVPipeResult<List<TrackedTarget>> targetList;
    private final Point[] rectPoints = new Point[4];

    ColoredShapePipeline() {
        settings = new ColoredShapePipelineSettings();
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, ColoredShapePipelineSettings settings) {

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

        OutputMatPipe.OutputMatParams outputMatParams =
                new OutputMatPipe.OutputMatParams(settings.outputShowThresholded);
        outputMatPipe.setParams(outputMatParams);

        SpeckleRejectPipe.SpeckleRejectParams speckleRejectParams =
                new SpeckleRejectPipe.SpeckleRejectParams(settings.contourSpecklePercentage);
        speckleRejectPipe.setParams(speckleRejectParams);

        FindContoursPipe.FindContoursParams findContoursParams =
                new FindContoursPipe.FindContoursParams();
        findContoursPipe.setParams(findContoursParams);

        FindPolygonPipeParams findPolygonPipeParams =
                new FindPolygonPipeParams(settings.accuracyPercentage);
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

        FilterShapesPipeParams filterShapesPipeParams =
                new FilterShapesPipeParams(
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
                new SortContoursPipe.SortContoursParams(settings.contourSortMode, frameStaticProperties, 5);
        sortContoursPipe.setParams(sortContoursParams);

        Collect2dTargetsPipe.Collect2dTargetsParams collect2dTargetsParams =
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        frameStaticProperties,
                        settings.offsetRobotOffsetMode,
                        settings.offsetDualLineM,
                        settings.offsetDualLineB,
                        settings.offsetCalibrationPoint.toPoint(),
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation);
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
                        settings.cameraCalibration, settings.cameraPitch, settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);

        Draw2dContoursPipe.Draw2dContoursParams draw2dContoursParams =
                new Draw2dContoursPipe.Draw2dContoursParams(settings.outputShowMultipleTargets);
        draw2dContoursParams.showShape = true;
        draw2dContoursParams.showMaximumBox = false;
        draw2dContoursParams.showRotatedBox = false;
        draw2dContoursParams.boxOutlineSize = 2;
        draw2dContoursPipe.setParams(draw2dContoursParams);

        Draw2dCrosshairPipe.Draw2dCrosshairParams draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.offsetRobotOffsetMode, settings.offsetCalibrationPoint);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        var draw3dContoursParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.cameraCalibration, settings.targetModel);
        draw3dTargetsPipe.setParams(draw3dContoursParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, ColoredShapePipelineSettings settings) {
        setPipeParams(frame.frameStaticProperties, settings);

        long sumPipeNanosElapsed = 0L;

        frame.image.getMat().copyTo(rawInputMat);

        CVPipeResult<Mat> rotateImageResult = rotateImagePipe.apply(frame.image.getMat());
        sumPipeNanosElapsed += rotateImageResult.nanosElapsed;

        CVPipeResult<Mat> erodeDilateResult = erodeDilatePipe.apply(rotateImageResult.result);
        sumPipeNanosElapsed += erodeDilateResult.nanosElapsed;

        CVPipeResult<Mat> hsvPipeResult = hsvPipe.apply(erodeDilateResult.result);
        sumPipeNanosElapsed += hsvPipeResult.nanosElapsed;

        outputMats.first = rawInputMat;
        outputMats.second = hsvPipeResult.result;

        CVPipeResult<Mat> outputMatResult = outputMatPipe.apply(outputMats);
        sumPipeNanosElapsed += outputMatResult.nanosElapsed;

        CVPipeResult<List<Contour>> findContoursResult = findContoursPipe.apply(hsvPipeResult.result);
        sumPipeNanosElapsed += findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.apply(findContoursResult.result);
        sumPipeNanosElapsed += speckleRejectResult.nanosElapsed;

        if (settings.desiredShape == ContourShape.Circle) {
            CVPipeResult<List<CVShape>> findCirclesResult =
                    findCirclesPipe.apply(Pair.of(hsvPipeResult.result, speckleRejectResult.result));
            sumPipeNanosElapsed += findCirclesResult.nanosElapsed;
            shapes = findCirclesResult.result;
        } else {
            CVPipeResult<List<CVShape>> findPolygonsResult =
                    findPolygonPipe.apply(speckleRejectResult.result);
            sumPipeNanosElapsed += findPolygonsResult.nanosElapsed;
            shapes = findPolygonsResult.result;
        }

        CVPipeResult<List<CVShape>> filterShapeResult = filterShapesPipe.apply(shapes);
        sumPipeNanosElapsed += filterShapeResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> groupContoursResult =
                groupContoursPipe.apply(
                        filterShapeResult.result.stream()
                                .map(CVShape::getContour)
                                .collect(Collectors.toList()));
        sumPipeNanosElapsed += groupContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.apply(groupContoursResult.result);
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.apply(sortContoursResult.result);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        if (settings.solvePNPEnabled && settings.desiredShape == ContourShape.Circle) {
            var cornerDetectionResult = cornerDetectionPipe.apply(collect2dTargetsResult.result);
            collect2dTargetsResult.result.forEach(
                    shape -> {
                        shape.getMinAreaRect().points(rectPoints);
                        shape.setCorners(Arrays.asList(rectPoints));
                    });
            sumPipeNanosElapsed += cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.apply(cornerDetectionResult.result);
            sumPipeNanosElapsed += solvePNPResult.nanosElapsed;

            targetList = solvePNPResult;
        } else {
            targetList = collect2dTargetsResult;
        }

        CVPipeResult<Mat> draw2dCrosshairResult =
                draw2dCrosshairPipe.apply(Pair.of(outputMatResult.result, targetList.result));
        sumPipeNanosElapsed += draw2dCrosshairResult.nanosElapsed;

        CVPipeResult<Mat> draw2dContoursResult =
                draw2dContoursPipe.apply(
                        Pair.of(draw2dCrosshairResult.result, collect2dTargetsResult.result));
        sumPipeNanosElapsed += draw2dContoursResult.nanosElapsed;

        if (settings.solvePNPEnabled && settings.desiredShape == ContourShape.Circle) {
            result =
                    draw3dTargetsPipe.apply(
                            Pair.of(draw2dCrosshairResult.result, collect2dTargetsResult.result));
            sumPipeNanosElapsed += result.nanosElapsed;
        } else {
            result = draw2dContoursResult;
        }

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                collect2dTargetsResult.result,
                new Frame(new CVMat(result.result), frame.frameStaticProperties));
    }
}

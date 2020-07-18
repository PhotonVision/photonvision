/*
 * Copyright (C) 2020 Photon Vision.
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
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.DualMat;
import org.photonvision.vision.pipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.Collect2dTargetsPipe;
import org.photonvision.vision.pipe.impl.CornerDetectionPipe;
import org.photonvision.vision.pipe.impl.Draw2dCrosshairPipe;
import org.photonvision.vision.pipe.impl.Draw2dTargetsPipe;
import org.photonvision.vision.pipe.impl.Draw3dTargetsPipe;
import org.photonvision.vision.pipe.impl.ErodeDilatePipe;
import org.photonvision.vision.pipe.impl.FilterContoursPipe;
import org.photonvision.vision.pipe.impl.FindContoursPipe;
import org.photonvision.vision.pipe.impl.GroupContoursPipe;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipe.impl.OutputMatPipe;
import org.photonvision.vision.pipe.impl.RotateImagePipe;
import org.photonvision.vision.pipe.impl.SolvePNPPipe;
import org.photonvision.vision.pipe.impl.SortContoursPipe;
import org.photonvision.vision.pipe.impl.SpeckleRejectPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;

/** Represents a pipeline for tracking retro-reflective targets. */
@SuppressWarnings({"UnusedAssignment", "DuplicatedCode"})
public class ReflectivePipeline extends CVPipeline<CVPipelineResult, ReflectivePipelineSettings> {

    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final ErodeDilatePipe erodeDilatePipe = new ErodeDilatePipe();
    private final HSVPipe hsvPipe = new HSVPipe();
    private final OutputMatPipe outputMatPipe = new OutputMatPipe();
    private final FindContoursPipe findContoursPipe = new FindContoursPipe();
    private final SpeckleRejectPipe speckleRejectPipe = new SpeckleRejectPipe();
    private final FilterContoursPipe filterContoursPipe = new FilterContoursPipe();
    private final GroupContoursPipe groupContoursPipe = new GroupContoursPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final CornerDetectionPipe cornerDetectionPipe = new CornerDetectionPipe();
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final Draw2dTargetsPipe draw2DTargetsPipe = new Draw2dTargetsPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();

    private final Mat rawInputMat = new Mat();
    private final DualMat outputMats = new DualMat();

    public ReflectivePipeline() {
        settings = new ReflectivePipelineSettings();
    }

    public ReflectivePipeline(ReflectivePipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, ReflectivePipelineSettings settings) {
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

        FindContoursPipe.FindContoursParams findContoursParams =
                new FindContoursPipe.FindContoursParams();
        findContoursPipe.setParams(findContoursParams);

        SpeckleRejectPipe.SpeckleRejectParams speckleRejectParams =
                new SpeckleRejectPipe.SpeckleRejectParams(settings.contourSpecklePercentage);
        speckleRejectPipe.setParams(speckleRejectParams);

        FilterContoursPipe.FilterContoursParams filterContoursParams =
                new FilterContoursPipe.FilterContoursParams(
                        settings.contourArea,
                        settings.contourRatio,
                        settings.contourFullness,
                        frameStaticProperties);
        filterContoursPipe.setParams(filterContoursParams);

        GroupContoursPipe.GroupContoursParams groupContoursParams =
                new GroupContoursPipe.GroupContoursParams(
                        settings.contourGroupingMode, settings.contourIntersection);
        groupContoursPipe.setParams(groupContoursParams);

        SortContoursPipe.SortContoursParams sortContoursParams =
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        frameStaticProperties,
                        settings.outputShowMultipleTargets ? 5 : 1); // TODO don't hardcode?
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

        Draw2dTargetsPipe.Draw2dContoursParams draw2dContoursParams =
                new Draw2dTargetsPipe.Draw2dContoursParams(settings.outputShowMultipleTargets);
        draw2DTargetsPipe.setParams(draw2dContoursParams);

        Draw2dCrosshairPipe.Draw2dCrosshairParams draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.offsetRobotOffsetMode, settings.offsetCalibrationPoint);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        var draw3dContoursParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.cameraCalibration, settings.targetModel);
        draw3dTargetsPipe.setParams(draw3dContoursParams);

        var solvePNPParams =
                new SolvePNPPipe.SolvePNPPipeParams(
                        settings.cameraCalibration, settings.cameraPitch, settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);
    }

    @Override
    public CVPipelineResult process(Frame frame, ReflectivePipelineSettings settings) {
        setPipeParams(frame.frameStaticProperties, settings);

        long sumPipeNanosElapsed = 0L;

        CVPipeResult<Mat> rotateImageResult = rotateImagePipe.apply(frame.image.getMat());
        sumPipeNanosElapsed += rotateImageResult.nanosElapsed;

        rawInputMat.release();
        frame.image.getMat().copyTo(rawInputMat);

        CVPipeResult<Mat> erodeDilateResult = erodeDilatePipe.apply(rotateImageResult.result);
        sumPipeNanosElapsed += erodeDilateResult.nanosElapsed;

        CVPipeResult<Mat> hsvPipeResult = hsvPipe.apply(erodeDilateResult.result);
        sumPipeNanosElapsed += hsvPipeResult.nanosElapsed;

        // the first is the raw input mat, the second is the HSVPipe result
        outputMats.first = rawInputMat;
        outputMats.second = hsvPipeResult.result;

        CVPipeResult<List<Contour>> findContoursResult = findContoursPipe.apply(hsvPipeResult.result);
        sumPipeNanosElapsed += findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.apply(findContoursResult.result);
        sumPipeNanosElapsed += speckleRejectResult.nanosElapsed;

        CVPipeResult<List<Contour>> filterContoursResult =
                filterContoursPipe.apply(speckleRejectResult.result);
        sumPipeNanosElapsed += filterContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> groupContoursResult =
                groupContoursPipe.apply(filterContoursResult.result);
        sumPipeNanosElapsed += groupContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.apply(groupContoursResult.result);
        sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.apply(sortContoursResult.result);
        sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        // 3d stuff
        if (settings.solvePNPEnabled) {
            var cornerDetectionResult = cornerDetectionPipe.apply(collect2dTargetsResult.result);
            sumPipeNanosElapsed += cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.apply(cornerDetectionResult.result);
            sumPipeNanosElapsed += solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.result;
        } else {
            targetList = collect2dTargetsResult.result;
        }

        // the first is the raw input mat, the second is the HSVPipe result
        CVPipeResult<Mat> drawOnInputResult, drawOnOutputResult;

        // Draw 2D Crosshair on input and output
        CVPipeResult<Mat> draw2dCrosshairResultOnInput =
                draw2dCrosshairPipe.apply(Pair.of(outputMats.first, targetList));
        sumPipeNanosElapsed += draw2dCrosshairResultOnInput.nanosElapsed;

        CVPipeResult<Mat> draw2dCrosshairResultOnOutput =
                draw2dCrosshairPipe.apply(Pair.of(outputMats.second, targetList));
        sumPipeNanosElapsed += draw2dCrosshairResultOnOutput.nanosElapsed;

        // Draw 2D contours on input and output
        CVPipeResult<Mat> draw2dContoursResultOnInput =
                draw2DTargetsPipe.apply(
                        Pair.of(draw2dCrosshairResultOnInput.result, collect2dTargetsResult.result));
        sumPipeNanosElapsed += draw2dCrosshairResultOnInput.nanosElapsed;

        CVPipeResult<Mat> draw2dContoursResultOnOutput =
                draw2DTargetsPipe.apply(
                        Pair.of(draw2dCrosshairResultOnOutput.result, collect2dTargetsResult.result));
        sumPipeNanosElapsed += draw2dContoursResultOnOutput.nanosElapsed;

        // Draw 3D Targets on input and output if necessary
        if (settings.solvePNPEnabled) {
            drawOnInputResult =
                    draw3dTargetsPipe.apply(
                            Pair.of(draw2dContoursResultOnInput.result, collect2dTargetsResult.result));
            sumPipeNanosElapsed += drawOnInputResult.nanosElapsed;

            drawOnOutputResult =
                    draw3dTargetsPipe.apply(
                            Pair.of(draw2dContoursResultOnOutput.result, collect2dTargetsResult.result));
            sumPipeNanosElapsed += drawOnOutputResult.nanosElapsed;
        } else {
            drawOnInputResult = draw2dContoursResultOnInput;
            drawOnOutputResult = draw2dContoursResultOnOutput;
        }

        // Convert single-channel HSV output mat to 3-channel BGR in preparation for streaming
        CVPipeResult<Mat> outputMatPipeResult = outputMatPipe.apply(outputMats.second);
        sumPipeNanosElapsed += outputMatPipeResult.nanosElapsed;

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                targetList,
                new Frame(new CVMat(outputMats.second), frame.frameStaticProperties),
                new Frame(new CVMat(outputMats.first), frame.frameStaticProperties));
    }
}

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
import org.apache.commons.lang3.tuple.Triple;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;

/** Represents a pipeline for tracking retro-reflective targets. */
@SuppressWarnings({"DuplicatedCode"})
public class ReflectivePipeline extends CVPipeline<CVPipelineResult, ReflectivePipelineSettings> {

    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final HSVPipe hsvPipe = new HSVPipe();
    private final FindContoursPipe findContoursPipe = new FindContoursPipe();
    private final SpeckleRejectPipe speckleRejectPipe = new SpeckleRejectPipe();
    private final FilterContoursPipe filterContoursPipe = new FilterContoursPipe();
    private final GroupContoursPipe groupContoursPipe = new GroupContoursPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final CornerDetectionPipe cornerDetectionPipe = new CornerDetectionPipe();
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();
    private final OutputMatPipe outputMatPipe = new OutputMatPipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final Draw2dTargetsPipe draw2dTargetsPipe = new Draw2dTargetsPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private Mat rawInputMat = new Mat();
    private final long[] pipeProfileNanos = new long[PipelineProfiler.ReflectivePipeCount];

    public ReflectivePipeline() {
        settings = new ReflectivePipelineSettings();
    }

    public ReflectivePipeline(ReflectivePipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, ReflectivePipelineSettings settings) {

        var dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        var rotateImageParams = new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        var hsvParams =
                new HSVPipe.HSVParams(settings.hsvHue, settings.hsvSaturation, settings.hsvValue);
        hsvPipe.setParams(hsvParams);
        PicamJNI.setThresholds(
                settings.hsvHue.getFirst() / 255d, settings.hsvSaturation.getFirst() / 255d, settings.hsvValue.getFirst() / 255d,
                settings.hsvHue.getSecond() / 255d, settings.hsvSaturation.getSecond() / 255d, settings.hsvValue.getSecond() / 255d
        );

        var findContoursParams = new FindContoursPipe.FindContoursParams();
        findContoursPipe.setParams(findContoursParams);

        var speckleRejectParams =
                new SpeckleRejectPipe.SpeckleRejectParams(settings.contourSpecklePercentage);
        speckleRejectPipe.setParams(speckleRejectParams);

        var filterContoursParams =
                new FilterContoursPipe.FilterContoursParams(
                        settings.contourArea,
                        settings.contourRatio,
                        settings.contourFullness,
                        frameStaticProperties);
        filterContoursPipe.setParams(filterContoursParams);

        var groupContoursParams =
                new GroupContoursPipe.GroupContoursParams(
                        settings.contourGroupingMode, settings.contourIntersection);
        groupContoursPipe.setParams(groupContoursParams);

        var sortContoursParams =
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets ? 5 : 1, // TODO don't hardcode?
                        frameStaticProperties);
        sortContoursPipe.setParams(sortContoursParams);

        var collect2dTargetsParams =
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties);
        collect2dTargetsPipe.setParams(collect2dTargetsParams);

        var cornerDetectionPipeParams =
                new CornerDetectionPipe.CornerDetectionPipeParameters(
                        settings.cornerDetectionStrategy,
                        settings.cornerDetectionUseConvexHulls,
                        settings.cornerDetectionExactSideCount,
                        settings.cornerDetectionSideCount,
                        settings.cornerDetectionAccuracyPercentage);
        cornerDetectionPipe.setParams(cornerDetectionPipeParams);

        var draw2DTargetsParams =
                new Draw2dTargetsPipe.Draw2dTargetsParams(
                        settings.outputShouldDraw, settings.outputShowMultipleTargets);
        draw2dTargetsPipe.setParams(draw2DTargetsParams);

        var draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.outputShouldDraw,
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        frameStaticProperties);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        var draw3dTargetsParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.outputShouldDraw,
                        frameStaticProperties.cameraCalibration,
                        settings.targetModel);
        draw3dTargetsPipe.setParams(draw3dTargetsParams);

        var solvePNPParams =
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration,
                        frameStaticProperties.cameraPitch,
                        settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);
    }

    @Override
    public CVPipelineResult process(Frame frame, ReflectivePipelineSettings settings) {
        setPipeParams(frame.frameStaticProperties, settings);

        long sumPipeNanosElapsed = 0L;

        CVPipeResult<Mat> hsvPipeResult;
        if (frame.image.getMat().channels() != 1) {
            var rotateImageResult = rotateImagePipe.run(frame.image.getMat());
            sumPipeNanosElapsed += pipeProfileNanos[0] = rotateImageResult.nanosElapsed;

            // TODO: make this a pipe?
            long inputCopyStartNanos = System.nanoTime();
            rawInputMat.release();
            frame.image.getMat().copyTo(rawInputMat);
            long inputCopyElapsedNanos = System.nanoTime() - inputCopyStartNanos;
            sumPipeNanosElapsed += pipeProfileNanos[1] = inputCopyElapsedNanos;

            hsvPipeResult = hsvPipe.run(rawInputMat);
            sumPipeNanosElapsed += hsvPipeResult.nanosElapsed;
            pipeProfileNanos[3] = pipeProfileNanos[3] = hsvPipeResult.nanosElapsed;
        } else {
            rawInputMat = frame.image.getMat();

            // We can skip a few steps if the image is single channel, because we've already done them on the GPU
            hsvPipeResult = new CVPipeResult<>();
            hsvPipeResult.output = frame.image.getMat();
            hsvPipeResult.nanosElapsed = System.nanoTime() - frame.timestampNanos;

            sumPipeNanosElapsed = pipeProfileNanos[0] = hsvPipeResult.nanosElapsed;
        }

        CVPipeResult<List<Contour>> findContoursResult = findContoursPipe.run(hsvPipeResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[3] = findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.run(findContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[4] = speckleRejectResult.nanosElapsed;

        CVPipeResult<List<Contour>> filterContoursResult =
                filterContoursPipe.run(speckleRejectResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[5] = filterContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> groupContoursResult =
                groupContoursPipe.run(filterContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[6] = groupContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(groupContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[7] = sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[8] = collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        // 3d stuff
        if (settings.solvePNPEnabled) {
            var cornerDetectionResult = cornerDetectionPipe.run(collect2dTargetsResult.output);
            sumPipeNanosElapsed += pipeProfileNanos[9] = cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.run(cornerDetectionResult.output);
            sumPipeNanosElapsed += pipeProfileNanos[10] = solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.output;
        } else {
            pipeProfileNanos[9] = 0;
            pipeProfileNanos[10] = 0;
            targetList = collect2dTargetsResult.output;
        }

        // Calculate FPS for drawing
        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;
        sumPipeNanosElapsed += fpsResult.nanosElapsed;

        if (frame.image.getMat().channels() != 1) {

            // Convert single-channel HSV output mat to 3-channel BGR in preparation for streaming
            var outputMatPipeResult = outputMatPipe.run(hsvPipeResult.output);
            sumPipeNanosElapsed += pipeProfileNanos[12] = outputMatPipeResult.nanosElapsed;

            // Draw 2D Crosshair on input and output
            var draw2dCrosshairResultOnInput = draw2dCrosshairPipe.run(Pair.of(rawInputMat, targetList));
            sumPipeNanosElapsed += pipeProfileNanos[13] = draw2dCrosshairResultOnInput.nanosElapsed;

            var draw2dCrosshairResultOnOutput =
                    draw2dCrosshairPipe.run(Pair.of(hsvPipeResult.output, targetList));
            sumPipeNanosElapsed += pipeProfileNanos[14] = draw2dCrosshairResultOnOutput.nanosElapsed;

            // Draw 2D contours on input and output
            var draw2dTargetsOnInput =
                    draw2dTargetsPipe.run(Triple.of(rawInputMat, collect2dTargetsResult.output, fps));
            sumPipeNanosElapsed += pipeProfileNanos[15] = draw2dTargetsOnInput.nanosElapsed;

            var draw2dTargetsOnOutput =
                    draw2dTargetsPipe.run(Triple.of(hsvPipeResult.output, collect2dTargetsResult.output, fps));
            sumPipeNanosElapsed += pipeProfileNanos[16] = draw2dTargetsOnOutput.nanosElapsed;

            // Draw 3D Targets on input and output if necessary
            if (settings.solvePNPEnabled) {
                var drawOnInputResult =
                        draw3dTargetsPipe.run(Pair.of(rawInputMat, collect2dTargetsResult.output));
                sumPipeNanosElapsed += pipeProfileNanos[17] = drawOnInputResult.nanosElapsed;

                var drawOnOutputResult =
                        draw3dTargetsPipe.run(Pair.of(hsvPipeResult.output, collect2dTargetsResult.output));
                sumPipeNanosElapsed += pipeProfileNanos[18] = drawOnOutputResult.nanosElapsed;
            } else {
                pipeProfileNanos[17] = 0;
                pipeProfileNanos[18] = 0;
            }
        }

        PipelineProfiler.printReflectiveProfile(pipeProfileNanos);

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                targetList,
                new Frame(new CVMat(hsvPipeResult.output), frame.frameStaticProperties),
                new Frame(new CVMat(rawInputMat), frame.frameStaticProperties));
    }
}

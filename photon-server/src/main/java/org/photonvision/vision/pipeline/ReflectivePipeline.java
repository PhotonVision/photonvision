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
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.frame.Frame;
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
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private final long[] pipeProfileNanos = new long[PipelineProfiler.ReflectivePipeCount];

    public ReflectivePipeline() {
        settings = new ReflectivePipelineSettings();
    }

    public ReflectivePipeline(ReflectivePipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {

        var dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        var rotateImageParams = new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && PicamJNI.isSupported()) {
            PicamJNI.setThresholds(
                    settings.hsvHue.getFirst() / 180d,
                    settings.hsvSaturation.getFirst() / 255d,
                    settings.hsvValue.getFirst() / 255d,
                    settings.hsvHue.getSecond() / 180d,
                    settings.hsvSaturation.getSecond() / 255d,
                    settings.hsvValue.getSecond() / 255d);

            PicamJNI.setRotation(settings.inputImageRotationMode.value);
            PicamJNI.setShouldCopyColor(settings.inputShouldShow);
        } else {
            var hsvParams =
                    new HSVPipe.HSVParams(settings.hsvHue, settings.hsvSaturation, settings.hsvValue);
            hsvPipe.setParams(hsvParams);
        }

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

        var solvePNPParams =
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration,
                        frameStaticProperties.cameraPitch,
                        settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);
    }

    @Override
    public CVPipelineResult process(Frame frame, ReflectivePipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        CVPipeResult<Mat> hsvPipeResult;
        Mat rawInputMat;
        if (frame.image.getMat().channels() != 1) {
            var rotateImageResult = rotateImagePipe.run(frame.image.getMat());
            sumPipeNanosElapsed += pipeProfileNanos[0] = rotateImageResult.nanosElapsed;

            rawInputMat = frame.image.getMat();

            hsvPipeResult = hsvPipe.run(rawInputMat);
            sumPipeNanosElapsed += hsvPipeResult.nanosElapsed;
            pipeProfileNanos[1] = pipeProfileNanos[1] = hsvPipeResult.nanosElapsed;
        } else {
            long inputMatPtr = PicamJNI.grabFrame(true);
            if (inputMatPtr != 0) rawInputMat = new Mat(inputMatPtr);
            else rawInputMat = frame.image.getMat();

            // We can skip a few steps if the image is single channel because we've already done them on
            // the GPU
            hsvPipeResult = new CVPipeResult<>();
            hsvPipeResult.output = frame.image.getMat();
            hsvPipeResult.nanosElapsed = System.nanoTime() - frame.timestampNanos;

            sumPipeNanosElapsed = pipeProfileNanos[1] = hsvPipeResult.nanosElapsed;
        }

        CVPipeResult<List<Contour>> findContoursResult = findContoursPipe.run(hsvPipeResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[2] = findContoursResult.nanosElapsed;

        CVPipeResult<List<Contour>> speckleRejectResult =
                speckleRejectPipe.run(findContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[3] = speckleRejectResult.nanosElapsed;

        CVPipeResult<List<Contour>> filterContoursResult =
                filterContoursPipe.run(speckleRejectResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[4] = filterContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> groupContoursResult =
                groupContoursPipe.run(filterContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[5] = groupContoursResult.nanosElapsed;

        CVPipeResult<List<PotentialTarget>> sortContoursResult =
                sortContoursPipe.run(groupContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[6] = sortContoursResult.nanosElapsed;

        CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                collect2dTargetsPipe.run(sortContoursResult.output);
        sumPipeNanosElapsed += pipeProfileNanos[7] = collect2dTargetsResult.nanosElapsed;

        List<TrackedTarget> targetList;

        // 3d stuff
        if (settings.solvePNPEnabled) {
            var cornerDetectionResult = cornerDetectionPipe.run(collect2dTargetsResult.output);
            sumPipeNanosElapsed += pipeProfileNanos[8] = cornerDetectionResult.nanosElapsed;

            var solvePNPResult = solvePNPPipe.run(cornerDetectionResult.output);
            sumPipeNanosElapsed += pipeProfileNanos[9] = solvePNPResult.nanosElapsed;

            targetList = solvePNPResult.output;
        } else {
            pipeProfileNanos[8] = 0;
            pipeProfileNanos[9] = 0;
            targetList = collect2dTargetsResult.output;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        PipelineProfiler.printReflectiveProfile(pipeProfileNanos);

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                fps,
                targetList,
                new Frame(new CVMat(hsvPipeResult.output), frame.frameStaticProperties),
                new Frame(new CVMat(rawInputMat), frame.frameStaticProperties));
    }
}

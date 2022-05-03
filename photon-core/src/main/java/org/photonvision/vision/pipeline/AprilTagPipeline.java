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

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.apriltag.AprilTagDetectorParams;
import org.photonvision.vision.apriltag.DetectionResult;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

@SuppressWarnings("DuplicatedCode")
public class AprilTagPipeline extends CVPipeline<CVPipelineResult, AprilTagPipelineSettings> {
    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final GrayscalePipe grayscalePipe = new GrayscalePipe();
    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final SolvePNPPipe solvePNPPipe = new SolvePNPPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    public AprilTagPipeline() {
        settings = new AprilTagPipelineSettings();
    }

    public AprilTagPipeline(AprilTagPipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && PicamJNI.isSupported()) {
            // TODO: Picam grayscale
            PicamJNI.setRotation(settings.inputImageRotationMode.value);
            PicamJNI.setShouldCopyColor(true); // need the color image to grayscale
        }

        AprilTagDetectorParams aprilTagDetectionParams =
                new AprilTagDetectorParams(
                        settings.tagFamily,
                        settings.decimate,
                        settings.blur,
                        settings.threads,
                        settings.debug,
                        settings.refineEdges);
        aprilTagDetectionPipe.setParams(aprilTagDetectionParams);

        var solvePNPParams =
                new SolvePNPPipe.SolvePNPPipeParams(
                        frameStaticProperties.cameraCalibration,
                        frameStaticProperties.cameraPitch,
                        settings.targetModel);
        solvePNPPipe.setParams(solvePNPParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, AprilTagPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        CVPipeResult<Mat> grayscalePipeResult;
        Mat rawInputMat;
        boolean inputSingleChannel = frame.image.getMat().channels() == 1;

        if (inputSingleChannel) {
            rawInputMat = new Mat(PicamJNI.grabFrame(true));
            frame.image.getMat().release(); // release the 8bit frame ASAP.
        } else {
            rawInputMat = frame.image.getMat();
            var rotateImageResult = rotateImagePipe.run(rawInputMat);
            sumPipeNanosElapsed += rotateImageResult.nanosElapsed;
        }

        grayscalePipeResult = grayscalePipe.run(rawInputMat);
        sumPipeNanosElapsed += grayscalePipeResult.nanosElapsed;

        List<TrackedTarget> targetList;
        CVPipeResult<List<DetectionResult>> tagDetectionPipeResult;

        tagDetectionPipeResult = aprilTagDetectionPipe.run(grayscalePipeResult.output);
        grayscalePipeResult.output.release();
        sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        targetList = new ArrayList<>();
        for (DetectionResult detection : tagDetectionPipeResult.output) {
            // populate the target list
            // Challenge here is that TrackedTarget functions with OpenCV Contour

            TrackedTarget target =
                    new TrackedTarget(
                            detection,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties));
            targetList.add(target);
        }

        if (settings.solvePNPEnabled) {
            targetList = solvePNPPipe.run(targetList).output;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        var inputFrame = new Frame(new CVMat(rawInputMat), frameStaticProperties);
        // empty output frame
        var outputFrame =
                Frame.emptyFrame(frameStaticProperties.imageWidth, frameStaticProperties.imageHeight);

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targetList, outputFrame, inputFrame);
    }
}

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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
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
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    public AprilTagPipeline() {
        settings = new AprilTagPipelineSettings();
    }

    public AprilTagPipeline(AprilTagPipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        // Sanitize thread count - not supported to have fewer than 1 threads
        settings.threads = Math.max(1, settings.threads);

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

        // TODO (HACK): tag width is Fun because it really belongs in the "target model"
        // We need the tag width for the JNI to figure out target pose, but we need a
        // target model for the draw 3d targets pipeline to work...

        // for now, hard code tag width based on enum value
        double tagWidth = 0.16; // guess at 200mm??
        switch (settings.targetModel) {
            case k200mmAprilTag:
                {
                    tagWidth = Units.inchesToMeters(3.25 * 2);
                    break;
                }
            default:
                {
                    break;
                }
        }

        aprilTagDetectionPipe.setParams(
                new AprilTagDetectionPipeParams(
                        aprilTagDetectionParams,
                        frameStaticProperties.cameraCalibration,
                        settings.numIterations,
                        tagWidth));
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

        var inputFrame = new Frame(new CVMat(rawInputMat), frameStaticProperties);

        grayscalePipeResult = grayscalePipe.run(rawInputMat);
        sumPipeNanosElapsed += grayscalePipeResult.nanosElapsed;

        var outputFrame = new Frame(new CVMat(grayscalePipeResult.output), frameStaticProperties);

        List<TrackedTarget> targetList;
        CVPipeResult<List<DetectionResult>> tagDetectionPipeResult;

        // Use the solvePNP Enabled flag to enable native pose estimation
        aprilTagDetectionPipe.setNativePoseEstimationEnabled(settings.solvePNPEnabled);

        tagDetectionPipeResult = aprilTagDetectionPipe.run(grayscalePipeResult.output);
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

            var correctedPose = MathUtils.convertOpenCVtoPhotonPose(target.getCameraToTarget3d());
            target.setCameraToTarget3d(
                    new Transform3d(correctedPose.getTranslation(), correctedPose.getRotation()));

            targetList.add(target);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targetList, outputFrame, inputFrame);
    }
}

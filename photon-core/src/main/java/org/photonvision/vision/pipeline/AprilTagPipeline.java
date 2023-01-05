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

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.apriltag.AprilTagPoseEstimator.Config;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

@SuppressWarnings("DuplicatedCode")
public class AprilTagPipeline extends CVPipeline<CVPipelineResult, AprilTagPipelineSettings> {
    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final AprilTagPoseEstimatorPipe poseEstimatorPipe = new AprilTagPoseEstimatorPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.GREYSCALE;

    public AprilTagPipeline() {
        super(PROCESSING_TYPE);
        settings = new AprilTagPipelineSettings();
    }

    public AprilTagPipeline(AprilTagPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        // Sanitize thread count - not supported to have fewer than 1 threads
        settings.threads = Math.max(1, settings.threads);

        // if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && LibCameraJNI.isSupported()) {
        //     // TODO: Picam grayscale
        //     LibCameraJNI.setRotation(settings.inputImageRotationMode.value);
        //     // LibCameraJNI.setShouldCopyColor(true); // need the color image to grayscale
        // }

        // TODO (HACK): tag width is Fun because it really belongs in the "target model"
        // We need the tag width for the JNI to figure out target pose, but we need a
        // target model for the draw 3d targets pipeline to work...

        // for now, hard code tag width based on enum value
        double tagWidth = Units.inchesToMeters(3 * 2); // for 6in 16h5 tag.

        // AprilTagDetectorParams aprilTagDetectionParams =
        //         new AprilTagDetectorParams(
        //                 settings.tagFamily,
        //                 settings.decimate,
        //                 settings.blur,
        //                 settings.threads,
        //                 settings.debug,
        //                 settings.refineEdges);

        var config = new AprilTagDetector.Config();
        config.numThreads = settings.threads;
        config.refineEdges = settings.refineEdges;
        config.quadSigma = (float) settings.blur;
        config.quadDecimate = settings.decimate;
        aprilTagDetectionPipe.setParams(new AprilTagDetectionPipeParams(settings.tagFamily, config));

        if (frameStaticProperties.cameraCalibration != null) {
            var cameraMatrix = frameStaticProperties.cameraCalibration.getCameraIntrinsicsMat();
            if (cameraMatrix != null) {
                var cx = cameraMatrix.get(0, 2)[0];
                var cy = cameraMatrix.get(1, 2)[0];
                var fx = cameraMatrix.get(0, 0)[0];
                var fy = cameraMatrix.get(1, 1)[0];

                poseEstimatorPipe.setParams(
                        new AprilTagPoseEstimatorPipeParams(
                                new Config(tagWidth, fx, fy, cx, cy),
                                frameStaticProperties.cameraCalibration,
                                settings.numIterations));
            }
        }
    }

    @Override
    protected CVPipelineResult process(Frame frame, AprilTagPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        List<TrackedTarget> targetList;

        // Use the solvePNP Enabled flag to enable native pose estimation
        aprilTagDetectionPipe.setNativePoseEstimationEnabled(settings.solvePNPEnabled);

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // TODO so all cameras should give us ADAPTIVE_THRESH -- how should we handle if not?
            return new CVPipelineResult(0, 0, List.of());
        }

        CVPipeResult<List<AprilTagDetection>> tagDetectionPipeResult;
        tagDetectionPipeResult = aprilTagDetectionPipe.run(frame.processedImage);
        sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        targetList = new ArrayList<>();
        for (AprilTagDetection detection : tagDetectionPipeResult.output) {
            // TODO this should be in a pipe, not in the top level here (Matt)
            if (detection.getDecisionMargin() < settings.decisionMargin) continue;
            if (detection.getHamming() > settings.hammingDist) continue;

            AprilTagPoseEstimate tagPoseEstimate = null;
            if (settings.solvePNPEnabled) {
                var poseResult = poseEstimatorPipe.run(detection);
                sumPipeNanosElapsed += poseResult.nanosElapsed;
                tagPoseEstimate = poseResult.output;
            }

            // populate the target list
            // Challenge here is that TrackedTarget functions with OpenCV Contour
            TrackedTarget target =
                    new TrackedTarget(
                            detection,
                            tagPoseEstimate,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties));

            var correctedBestPose = MathUtils.convertOpenCVtoPhotonPose(target.getBestCameraToTarget3d());
            var correctedAltPose = MathUtils.convertOpenCVtoPhotonPose(target.getAltCameraToTarget3d());

            target.setBestCameraToTarget3d(
                    new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));
            target.setAltCameraToTarget3d(
                    new Transform3d(correctedAltPose.getTranslation(), correctedAltPose.getRotation()));

            targetList.add(target);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targetList, frame);
    }
}

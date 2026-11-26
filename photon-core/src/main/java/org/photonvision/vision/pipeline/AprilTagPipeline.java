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
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.configuration.ConfigProvider;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.estimation.TargetModel;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe.AprilTagDetectionPipeParams;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.ArucoDetectionPipe;
import org.photonvision.vision.pipe.impl.ArucoDetectionPipeParams;
import org.photonvision.vision.pipe.impl.ArucoPoseEstimatorPipe;
import org.photonvision.vision.pipe.impl.ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe.MultiTargetPNPPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;
import org.wpilib.math.geometry.CoordinateSystem;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.util.Units;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagDetector;
import org.wpilib.vision.apriltag.AprilTagPoseEstimate;
import org.wpilib.vision.apriltag.AprilTagPoseEstimator.Config;

public class AprilTagPipeline extends CVPipeline<CVPipelineResult, AprilTagPipelineSettings> {
    private static final Logger logger = new Logger(AprilTagPipeline.class, LogGroup.VisionModule);

    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final ArucoDetectionPipe arucoDetectionPipe = new ArucoDetectionPipe();
    private final AprilTagPoseEstimatorPipe singleTagPoseEstimatorPipe =
            new AprilTagPoseEstimatorPipe();
    private final ArucoPoseEstimatorPipe arucoPoseEstimatorPipe = new ArucoPoseEstimatorPipe();
    private final MultiTargetPNPPipe multiTagPNPPipe = new MultiTargetPNPPipe();
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

        if (settings.algorithm == FiducialAlgorithm.Aruco) {
            setArucoPipeParams();
        } else {
            setAprilTagPipeParams();
        }
    }

    private void setAprilTagPipeParams() {
        // for now, hard code tag width based on enum value
        // From 2024 best guess is 6.5
        double tagWidth = Units.inchesToMeters(6.5);
        TargetModel tagModel = TargetModel.kAprilTag36h11;
        if (settings.tagFamily == AprilTagFamily.kTag16h5) {
            // 2023 tag, 6in
            tagWidth = Units.inchesToMeters(6);
            tagModel = TargetModel.kAprilTag16h5;
        }

        var config = new AprilTagDetector.Config();
        config.numThreads = settings.threads;
        config.refineEdges = settings.refineEdges;
        config.quadSigma = (float) settings.blur;
        config.quadDecimate = settings.decimate;

        var quadParams = new AprilTagDetector.QuadThresholdParameters();
        // 5 was the default minClusterPixels in WPILib prior to 2025
        // increasing it causes detection problems when decimate > 1
        quadParams.minClusterPixels = 5;
        // these are the same as the values in WPILib 2025
        // setting them here to prevent upstream changes from changing behavior of the detector
        quadParams.maxNumMaxima = 10;
        quadParams.criticalAngle = 45 * Math.PI / 180.0;
        quadParams.maxLineFitMSE = 10.0f;
        quadParams.minWhiteBlackDiff = 5;
        quadParams.deglitch = false;

        aprilTagDetectionPipe.setParams(
                new AprilTagDetectionPipeParams(settings.tagFamily, config, quadParams));

        if (frameStaticProperties.cameraCalibration != null) {
            var cameraMatrix = frameStaticProperties.cameraCalibration.getCameraIntrinsicsMat();
            if (cameraMatrix != null && cameraMatrix.rows() > 0) {
                var cx = cameraMatrix.get(0, 2)[0];
                var cy = cameraMatrix.get(1, 2)[0];
                var fx = cameraMatrix.get(0, 0)[0];
                var fy = cameraMatrix.get(1, 1)[0];

                singleTagPoseEstimatorPipe.setParams(
                        new AprilTagPoseEstimatorPipeParams(
                                new Config(tagWidth, fx, fy, cx, cy),
                                frameStaticProperties.cameraCalibration,
                                settings.numIterations));

                // TODO global state ew
                var field = ConfigProvider.getInstance().getConfig().getFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, field, tagModel));
            }
        }
    }

    private void setArucoPipeParams() {
        var params = new ArucoDetectionPipeParams();
        // sanitize and record settings

        // for now, hard code tag width based on enum value
        // 2023/other: best guess is 6in
        double tagWidth = Units.inchesToMeters(6);
        TargetModel tagModel = TargetModel.kAprilTag16h5;

        params.tagFamily =
                switch (settings.tagFamily) {
                    case kTag36h11 -> {
                        // 2024 tag, 6.5in
                        tagWidth = Units.inchesToMeters(6.5);
                        tagModel = TargetModel.kAprilTag36h11;
                        yield Objdetect.DICT_APRILTAG_36h11;
                    }
                    case kTag16h5 -> {
                        // 2024 tag, 6.5in
                        tagWidth = Units.inchesToMeters(6);
                        tagModel = TargetModel.kAprilTag16h5;
                        yield Objdetect.DICT_APRILTAG_16h5;
                    }
                };

        int threshMinSize = Math.max(3, settings.threshWinSizes.getFirst());
        settings.threshWinSizes.setFirst(threshMinSize);
        params.threshMinSize = threshMinSize;
        int threshStepSize = Math.max(2, settings.threshStepSize);
        settings.threshStepSize = threshStepSize;
        params.threshStepSize = threshStepSize;
        int threshMaxSize = Math.max(threshMinSize, settings.threshWinSizes.getSecond());
        settings.threshWinSizes.setSecond(threshMaxSize);
        params.threshMaxSize = threshMaxSize;
        params.threshConstant = settings.threshConstant;

        params.useCornerRefinement = settings.useCornerRefinement;
        params.refinementMaxIterations = settings.refineNumIterations;
        params.refinementMinErrorPx = settings.refineMinErrorPx;
        params.useAruco3 = settings.useAruco3;
        params.aruco3MinMarkerSideRatio = settings.aruco3MinMarkerSideRatio;
        params.aruco3MinCanonicalImgSide = settings.aruco3MinCanonicalImgSide;
        arucoDetectionPipe.setParams(params);

        if (frameStaticProperties.cameraCalibration != null) {
            var cameraMatrix = frameStaticProperties.cameraCalibration.getCameraIntrinsicsMat();
            if (cameraMatrix != null && cameraMatrix.rows() > 0) {
                var estimatorParams =
                        new ArucoPoseEstimatorPipeParams(frameStaticProperties.cameraCalibration, tagWidth);
                arucoPoseEstimatorPipe.setParams(estimatorParams);

                // TODO global state ew
                var field = ConfigProvider.getInstance().getConfig().getFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, field, tagModel));
            }
        }
    }

    @Override
    protected CVPipelineResult process(Frame frame, AprilTagPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // We asked for a GREYSCALE frame, but didn't get one -- best we can do is give up
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
        }

        List<TrackedTarget> targetList;
        Optional<MultiTargetPNPResult> multiTagResult;

        if (settings.algorithm == FiducialAlgorithm.Aruco) {
            // If we want to debug the thresholding steps, draw the first step to the color image
            if (settings.debugThreshold) {
                drawThresholdFrame(
                        frame.processedImage.getMat(),
                        frame.colorImage.getMat(),
                        settings.threshWinSizes.getFirst(),
                        settings.threshConstant);
            }

            CVPipeResult<List<ArucoDetectionResult>> tagDetectionPipeResult =
                    arucoDetectionPipe.run(frame.processedImage);
            sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

            targetList = new ArrayList<>();
            for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
                // Populate target list for multitag
                // (TODO: Address circular dependencies. Multitag only requires corners and IDs, this should
                // not be necessary.)
                targetList.add(
                        new TrackedTarget(
                                detection,
                                null,
                                new TargetCalculationParameters(
                                        false, null, null, null, null, frameStaticProperties)));
            }

            // Do multi-tag pose estimation
            multiTagResult = Optional.empty();
            if (settings.solvePNPEnabled && settings.doMultiTarget) {
                var multiTagOutput = multiTagPNPPipe.run(targetList);
                sumPipeNanosElapsed += multiTagOutput.nanosElapsed;
                multiTagResult = multiTagOutput.output;
            }

            // Do single-tag pose estimation
            if (settings.solvePNPEnabled) {
                // Clear target list that was used for multitag so we can add target transforms
                targetList.clear();
                // TODO global state again ew
                var field = ConfigProvider.getInstance().getConfig().getFieldLayout();

                for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
                    AprilTagPoseEstimate tagPoseEstimate = null;
                    // Do single-tag estimation when "always enabled" or if a tag was not used for multitag
                    if (settings.doSingleTargetAlways
                            || !(multiTagResult.isPresent()
                                    && multiTagResult.get().fiducialIDsUsed.contains((short) detection.getId()))) {
                        var poseResult = arucoPoseEstimatorPipe.run(detection);
                        sumPipeNanosElapsed += poseResult.nanosElapsed;
                        tagPoseEstimate = poseResult.output;
                    }

                    // If single-tag estimation was not done, this is a multi-target tag from the layout
                    if (tagPoseEstimate == null && multiTagResult.isPresent()) {
                        // compute this tag's camera-to-tag transform using the multitag result
                        var tagPose = field.getTagPose(detection.getId());
                        if (tagPose.isPresent()) {
                            var camToTag =
                                    new Transform3d(
                                            new Pose3d().plus(multiTagResult.get().estimatedPose.best), tagPose.get());
                            // match expected OpenCV coordinate system
                            camToTag =
                                    CoordinateSystem.convert(camToTag, CoordinateSystem.NWU(), CoordinateSystem.EDN());

                            tagPoseEstimate = new AprilTagPoseEstimate(camToTag, camToTag, 0, 0);
                        }
                    }

                    // populate the target list
                    // Challenge here is that TrackedTarget functions with OpenCV Contour
                    TrackedTarget target =
                            new TrackedTarget(
                                    detection,
                                    tagPoseEstimate,
                                    new TargetCalculationParameters(
                                            false, null, null, null, null, frameStaticProperties));

                    var correctedBestPose =
                            MathUtils.convertOpenCVtoPhotonTransform(target.getBestCameraToTarget3d());
                    var correctedAltPose =
                            MathUtils.convertOpenCVtoPhotonTransform(target.getAltCameraToTarget3d());

                    target.setBestCameraToTarget3d(
                            new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));
                    target.setAltCameraToTarget3d(
                            new Transform3d(correctedAltPose.getTranslation(), correctedAltPose.getRotation()));

                    targetList.add(target);
                }
            }
        } else {
            CVPipeResult<List<AprilTagDetection>> tagDetectionPipeResult =
                    aprilTagDetectionPipe.run(frame.processedImage);
            sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

            List<AprilTagDetection> detections = tagDetectionPipeResult.output;
            List<AprilTagDetection> usedDetections = new ArrayList<>();
            targetList = new ArrayList<>();

            // Filter out detections based on pipeline settings
            for (AprilTagDetection detection : detections) {
                // TODO this should be in a pipe, not in the top level here (Matt)
                if (detection.getDecisionMargin() < settings.decisionMargin) continue;
                if (detection.getHamming() > settings.hammingDist) continue;

                usedDetections.add(detection);

                // Populate target list for multitag
                // (TODO: Address circular dependencies. Multitag only requires corners and IDs, this should
                // not be necessary.)
                TrackedTarget target =
                        new TrackedTarget(
                                detection,
                                null,
                                new TargetCalculationParameters(
                                        false, null, null, null, null, frameStaticProperties));

                targetList.add(target);
            }

            // Do multi-tag pose estimation
            multiTagResult = Optional.empty();
            if (settings.solvePNPEnabled && settings.doMultiTarget) {
                var multiTagOutput = multiTagPNPPipe.run(targetList);
                sumPipeNanosElapsed += multiTagOutput.nanosElapsed;
                multiTagResult = multiTagOutput.output;
            }

            // Do single-tag pose estimation
            if (settings.solvePNPEnabled) {
                // Clear target list that was used for multitag so we can add target transforms
                targetList.clear();
                // TODO global state again ew
                var field = ConfigProvider.getInstance().getConfig().getFieldLayout();

                for (AprilTagDetection detection : usedDetections) {
                    AprilTagPoseEstimate tagPoseEstimate = null;
                    // Do single-tag estimation when "always enabled" or if a tag was not used for multitag
                    if (settings.doSingleTargetAlways
                            || !(multiTagResult.isPresent()
                                    && multiTagResult.get().fiducialIDsUsed.contains((short) detection.getId()))) {
                        var poseResult = singleTagPoseEstimatorPipe.run(detection);
                        sumPipeNanosElapsed += poseResult.nanosElapsed;
                        tagPoseEstimate = poseResult.output;
                    }

                    // If single-tag estimation was not done, this is a multi-target tag from the layout
                    if (tagPoseEstimate == null && multiTagResult.isPresent()) {
                        // compute this tag's camera-to-tag transform using the multitag result
                        var tagPose = field.getTagPose(detection.getId());
                        if (tagPose.isPresent()) {
                            var camToTag =
                                    new Transform3d(
                                            new Pose3d().plus(multiTagResult.get().estimatedPose.best), tagPose.get());
                            // match expected AprilTag coordinate system
                            camToTag =
                                    CoordinateSystem.convert(camToTag, CoordinateSystem.NWU(), CoordinateSystem.EDN());
                            // (AprilTag expects Z axis going into tag)
                            camToTag =
                                    new Transform3d(
                                            camToTag.getTranslation(),
                                            new Rotation3d(0, Math.PI, 0).rotateBy(camToTag.getRotation()));
                            tagPoseEstimate = new AprilTagPoseEstimate(camToTag, camToTag, 0, 0);
                        }
                    }

                    // populate the target list
                    // Challenge here is that TrackedTarget functions with OpenCV Contour
                    TrackedTarget target =
                            new TrackedTarget(
                                    detection,
                                    tagPoseEstimate,
                                    new TargetCalculationParameters(
                                            false, null, null, null, null, frameStaticProperties));

                    var correctedBestPose =
                            MathUtils.convertOpenCVtoPhotonTransform(target.getBestCameraToTarget3d());
                    var correctedAltPose =
                            MathUtils.convertOpenCVtoPhotonTransform(target.getAltCameraToTarget3d());

                    target.setBestCameraToTarget3d(
                            new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));
                    target.setAltCameraToTarget3d(
                            new Transform3d(correctedAltPose.getTranslation(), correctedAltPose.getRotation()));

                    targetList.add(target);
                }
            }
        }

        if (targetList.size() > Packet.MAX_ARRAY_LEN) {
            logger.error(
                    "We have " + targetList.size() + " targets! Arbitrarily dropping some on the floor");
            targetList = targetList.subList(0, Packet.MAX_ARRAY_LEN);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                frame.sequenceID, sumPipeNanosElapsed, fps, targetList, multiTagResult, frame);
    }

    private void drawThresholdFrame(Mat greyMat, Mat outputMat, int windowSize, double constant) {
        if (windowSize % 2 == 0) windowSize++;
        Imgproc.adaptiveThreshold(
                greyMat,
                outputMat,
                255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY_INV,
                windowSize,
                constant);
    }

    @Override
    public void release() {
        aprilTagDetectionPipe.release();
        arucoDetectionPipe.release();
        singleTagPoseEstimatorPipe.release();
        arucoPoseEstimatorPipe.release();
        multiTagPNPPipe.release();
        calculateFPSPipe.release();
        super.release();
    }
}

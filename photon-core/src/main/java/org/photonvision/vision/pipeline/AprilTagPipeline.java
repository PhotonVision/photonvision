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
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.core.Rect2d;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.estimation.TargetModel;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe.AprilTagDetectionPipeParams;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.AprilTagROIDecodePipe;
import org.photonvision.vision.pipe.impl.AprilTagROIDetectionPipe;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe.MultiTargetPNPPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class AprilTagPipeline extends CVPipeline<CVPipelineResult, AprilTagPipelineSettings> {
    private static final Logger logger = new Logger(AprilTagPipeline.class, LogGroup.VisionModule);

    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final AprilTagPoseEstimatorPipe singleTagPoseEstimatorPipe =
            new AprilTagPoseEstimatorPipe();
    private final MultiTargetPNPPipe multiTagPNPPipe = new MultiTargetPNPPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    // ML-assisted detection pipes
    private final AprilTagROIDetectionPipe mlDetectionPipe = new AprilTagROIDetectionPipe();
    private final AprilTagROIDecodePipe mlDecodePipe = new AprilTagROIDecodePipe();
    private boolean mlAvailable = false;
    private boolean mlWasAvailable = false; 

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
                var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, atfl, tagModel));
            }
        }

        // ML-assisted detection configuration
        if (settings.useMLDetection) {
            mlAvailable = checkMLAvailability();

            if (mlAvailable) {
                Model apriltagModel = getAprilTagModel(settings.mlModelName);

                if (apriltagModel != null) {
                    mlDetectionPipe.setParams(
                            new AprilTagROIDetectionPipe.AprilTagROIDetectionParams(
                                    apriltagModel,
                                    settings.mlConfidenceThreshold,
                                    settings.mlNmsThreshold));

                    AprilTagROIDecodePipe.ROIDecodeParams decodeParams =
                            new AprilTagROIDecodePipe.ROIDecodeParams();
                    decodeParams.tagFamily = settings.tagFamily;
                    decodeParams.roiExpansionFactor = settings.mlRoiExpansionFactor;
                    decodeParams.maxHammingDistance = settings.hammingDist;
                    decodeParams.minDecisionMargin = settings.decisionMargin;
                    decodeParams.detectorConfig.numThreads = 1;
                    decodeParams.detectorConfig.refineEdges = settings.refineEdges;
                    decodeParams.detectorConfig.quadDecimate = 1; // No decimation for ROI - maximize accuracy
                    mlDecodePipe.setParams(decodeParams);

                    if (!mlWasAvailable) {
                        logger.info("ML-assisted AprilTag detection enabled");
                    }
                } else {
                    mlAvailable = false;
                    if (mlWasAvailable) {
                        logger.warn("ML-assisted detection enabled but no AprilTag model found");
                    }
                }
            } else {
                if (mlWasAvailable) {
                    logger.debug("ML-assisted detection not available on this platform");
                }
            }
        } else {
            mlAvailable = false;
        }
        mlWasAvailable = mlAvailable;
    }

    @Override
    protected CVPipelineResult process(Frame frame, AprilTagPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // We asked for a GREYSCALE frame, but didn't get one -- best we can do is give up
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
        }

        // Perform AprilTag detection (traditional or ML-assisted)
        List<AprilTagDetection> detections;
        long detectionNanos;

        if (settings.useMLDetection && mlAvailable) {
            // Use ML-assisted hybrid detection
            var mlDetectionResult = processMLHybrid(frame);
            detections = mlDetectionResult.detections;
            detectionNanos = mlDetectionResult.nanosElapsed;

            // Fallback to traditional detection if ML found nothing
            if (detections.isEmpty() && settings.mlFallbackToTraditional) {
                CVPipeResult<List<AprilTagDetection>> fallbackResult =
                        aprilTagDetectionPipe.run(frame.processedImage);
                detections = fallbackResult.output;
                detectionNanos += fallbackResult.nanosElapsed;
            }
        } else {
            // Use traditional detection
            CVPipeResult<List<AprilTagDetection>> tagDetectionPipeResult =
                    aprilTagDetectionPipe.run(frame.processedImage);
            detections = tagDetectionPipeResult.output;
            detectionNanos = tagDetectionPipeResult.nanosElapsed;
        }
        sumPipeNanosElapsed += detectionNanos;

        List<AprilTagDetection> usedDetections = new ArrayList<>();
        List<TrackedTarget> targetList = new ArrayList<>();

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
        Optional<MultiTargetPNPResult> multiTagResult = Optional.empty();
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
            var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();

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
                    var tagPose = atfl.getTagPose(detection.getId());
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
                                        new Rotation3d(0, Math.PI, 0).plus(camToTag.getRotation()));
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

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                frame.sequenceID, sumPipeNanosElapsed, fps, targetList, multiTagResult, frame);
    }

    /** Result container for ML hybrid detection */
    private static class MLDetectionResult {
        final List<AprilTagDetection> detections;
        final long nanosElapsed;

        MLDetectionResult(List<AprilTagDetection> detections, long nanosElapsed) {
            this.detections = detections;
            this.nanosElapsed = nanosElapsed;
        }
    }

    /**
     * Performs ML-assisted hybrid AprilTag detection. Stage 1: ML model detects ROIs Stage 2:
     * Traditional detector decodes tags within ROIs
     */
    private MLDetectionResult processMLHybrid(Frame frame) {
        long totalNanos = 0;

        // Stage 1: ML detection to find ROIs
        CVPipeResult<List<Rect2d>> mlResult = mlDetectionPipe.run(frame.colorImage);
        totalNanos += mlResult.nanosElapsed;
        List<Rect2d> rois = mlResult.output;

        if (rois.isEmpty()) {
            return new MLDetectionResult(new ArrayList<>(), totalNanos);
        }

        // Stage 2: Decode tags within ROIs using traditional detector
        AprilTagROIDecodePipe.ROIDecodeInput decodeInput =
                new AprilTagROIDecodePipe.ROIDecodeInput(frame.processedImage, rois);

        CVPipeResult<List<AprilTagDetection>> decodeResult = mlDecodePipe.run(decodeInput);
        totalNanos += decodeResult.nanosElapsed;

        return new MLDetectionResult(decodeResult.output, totalNanos);
    }

    /**
     * Checks if ML detection is available on the current platform. Currently supported: RK3588
     * (Orange Pi 5, Rock 5C, CoolPi 4B) and QCS6490 (Rubik Pi 3).
     */
    private boolean checkMLAvailability() {
        Platform platform = Platform.getCurrentPlatform();
        return platform == Platform.LINUX_QCS6490 || platform == Platform.LINUX_RK3588_64;
    }

    /**
     * Gets the AprilTag detection model, either by name or the default model.
     *
     * @param modelName Optional model name to look up, or null for default
     * @return The model, or null if no suitable model is found
     */
    private Model getAprilTagModel(String modelName) {
        NeuralNetworkModelManager manager = NeuralNetworkModelManager.getInstance();

        if (modelName != null && !modelName.isEmpty()) {
            return manager.getModelByName(modelName).orElse(null);
        }

        return manager.getDefaultAprilTagModel().orElse(null);
    }

    @Override
    public void release() {
        aprilTagDetectionPipe.release();
        singleTagPoseEstimatorPipe.release();
        mlDetectionPipe.release();
        mlDecodePipe.release();
        super.release();
    }
}

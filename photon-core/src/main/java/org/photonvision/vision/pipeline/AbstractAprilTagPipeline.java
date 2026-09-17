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
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.estimation.TargetModel;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe.MultiTargetPNPPipeParams;
import org.photonvision.vision.pipe.impl.SingleTagPoseEstimator;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;
import org.wpilib.math.geometry.CoordinateSystem;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.util.Units;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagPoseEstimate;
import org.wpilib.vision.apriltag.AprilTagPoseEstimator.Config;

/**
 * Everything an AprilTag pipeline needs once it already has a {@code List<AprilTagDetection>} for a
 * frame: decision-margin/hamming filtering, single- and multi-tag pose estimation, target-list
 * assembly, and FPS accounting. This is the ~110 lines {@link AprilTagPipeline} (the CPU/
 * libapriltag backend) and the Vulkan backend both need identically - detection is the only place
 * the two differ, so it's the only thing left abstract here.
 *
 * <p>Kept as a straight extraction of {@code AprilTagPipeline}'s pre-existing {@code process()}/
 * {@code setPipeParamsImpl()} bodies, not a rewrite - see the two subclasses for their (much
 * smaller) detector-specific halves.
 */
public abstract class AbstractAprilTagPipeline<S extends AprilTagPipelineSettingsBase>
        extends CVPipeline<CVPipelineResult, S> {
    private static final Logger logger =
            new Logger(AbstractAprilTagPipeline.class, LogGroup.VisionModule);

    // Not final: subclasses whose pose-estimator backend is switchable at runtime (e.g. {@link
    // VkAprilTagPipeline}'s CPU/VULKAN toggle) swap this out from {@link
    // #configureSingleTagPoseEstimatorBackend}, which - critically - is called from {@link
    // #setPipeParamsImpl} (an ordinary instance method invoked well after construction), never
    // from this class's own constructor, so there's no constructor-time-virtual-call hazard here.
    protected SingleTagPoseEstimator singleTagPoseEstimatorPipe = new AprilTagPoseEstimatorPipe();
    private final MultiTargetPNPPipe multiTagPNPPipe = new MultiTargetPNPPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    protected AbstractAprilTagPipeline() {
        super(FrameThresholdType.GREYSCALE);
    }

    /**
     * Runs this pipeline's detector on the frame's already-thresholded image. Implementations should
     * NOT apply the decisionMargin/hammingDist filtering below themselves - that's done once,
     * uniformly, in {@link #process}.
     */
    protected abstract CVPipeResult<List<AprilTagDetection>> runDetection(Frame frame);

    /**
     * Configures the concrete detector backend from {@code settings}. Called from {@link
     * #setPipeParamsImpl} before the shared pose-estimation config below runs.
     */
    protected abstract void setDetectorParams(S settings);

    /**
     * Hook for subclasses whose single-tag pose-estimator backend is switchable at runtime (e.g.
     * {@link VkAprilTagPipeline}'s CPU/VULKAN {@code poseEstimatorBackend} toggle): called at the
     * start of every {@link #setPipeParamsImpl}, before {@link #singleTagPoseEstimatorPipe} is
     * configured below, so an override can swap it out for a different {@link SingleTagPoseEstimator}
     * implementation first. No-op by default - {@link AprilTagPipeline} (the CPU backend) never needs
     * to swap its pose estimator.
     */
    protected void configureSingleTagPoseEstimatorBackend() {}

    @Override
    protected void setPipeParamsImpl() {
        setDetectorParams(settings);
        configureSingleTagPoseEstimatorBackend();

        // for now, hard code tag width based on enum value
        // From 2024 best guess is 6.5
        double tagWidth = Units.inchesToMeters(6.5);
        TargetModel tagModel = TargetModel.kAprilTag36h11;
        if (settings.tagFamily == AprilTagFamily.kTag16h5) {
            // 2023 tag, 6in
            tagWidth = Units.inchesToMeters(6);
            tagModel = TargetModel.kAprilTag16h5;
        }

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
                var field = ConfigManager.getInstance().getConfig().getFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, field, tagModel));
            }
        }
    }

    @Override
    protected CVPipelineResult process(Frame frame, S settings) {
        long sumPipeNanosElapsed = 0L;

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // We asked for a GREYSCALE frame, but didn't get one -- best we can do is give up
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
        }

        CVPipeResult<List<AprilTagDetection>> tagDetectionPipeResult = runDetection(frame);
        sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        List<AprilTagDetection> detections = tagDetectionPipeResult.output;
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
            var field = ConfigManager.getInstance().getConfig().getFieldLayout();

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

    @Override
    public void release() {
        singleTagPoseEstimatorPipe.release();
        multiTagPNPPipe.release();
        calculateFPSPipe.release();
        super.release();
    }
}

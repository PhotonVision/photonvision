/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.interpolation.TimeInterpolatableBuffer;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.numbers.N8;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.photonvision.estimation.TargetModel;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

/**
 * The PhotonPoseEstimator class filters or combines readings from all the AprilTags visible at a
 * given timestamp on the field to produce a single robot in field pose, using the strategy set
 * below. Example usage can be found in our apriltagExample example project.
 */
public class PhotonPoseEstimator {
    private static int InstanceCount = 0;

    /** Position estimation strategies that can be used by the {@link PhotonPoseEstimator} class. */
    public enum PoseStrategy {
        /** Choose the Pose with the lowest ambiguity. */
        LOWEST_AMBIGUITY,

        /** Choose the Pose which is closest to the camera height. */
        CLOSEST_TO_CAMERA_HEIGHT,

        /** Choose the Pose which is closest to a set Reference position. */
        CLOSEST_TO_REFERENCE_POSE,

        /** Choose the Pose which is closest to the last pose calculated */
        CLOSEST_TO_LAST_POSE,

        /** Return the average of the best target poses using ambiguity as weight. */
        AVERAGE_BEST_TARGETS,

        /**
         * Use all visible tags to compute a single pose estimate on coprocessor. This option needs to
         * be enabled on the PhotonVision web UI as well.
         */
        MULTI_TAG_PNP_ON_COPROCESSOR,

        /**
         * Use all visible tags to compute a single pose estimate. This runs on the RoboRIO, and can
         * take a lot of time.
         */
        MULTI_TAG_PNP_ON_RIO,

        /**
         * Use distance data from best visible tag to compute a Pose. This runs on the RoboRIO in order
         * to access the robot's yaw heading, and MUST have addHeadingData called every frame so heading
         * data is up-to-date.
         *
         * <p>Yields a Pose2d in estimatedRobotPose (0 for z, roll, pitch)
         *
         * <p>https://www.chiefdelphi.com/t/frc-6328-mechanical-advantage-2025-build-thread/477314/98
         */
        PNP_DISTANCE_TRIG_SOLVE,

        /**
         * Solve a constrained version of the Perspective-n-Point problem with the robot's drivebase
         * flat on the floor. This computation takes place on the RoboRIO, and typically takes not more
         * than 2ms. See {@link PhotonPoseEstimator.ConstrainedSolvepnpParams} and {@link
         * org.photonvision.jni.ConstrainedSolvepnpJni} for details and tuning handles this strategy
         * exposes. This strategy needs addHeadingData called every frame so heading data is up-to-date.
         * If Multi-Tag PNP is enabled on the coprocessor, it will be used to provide an initial seed to
         * the optimization algorithm -- otherwise, the multi-tag fallback strategy will be used as the
         * seed.
         */
        CONSTRAINED_SOLVEPNP
    }

    /**
     * Tuning handles we have over the CONSTRAINED_SOLVEPNP {@link PhotonPoseEstimator.PoseStrategy}.
     * Internally, the cost function is a sum-squared of pixel reprojection error + (optionally)
     * heading error * heading scale factor.
     *
     * @param headingFree If true, heading is completely free to vary. If false, heading excursions
     *     from the provided heading measurement will be penalized
     * @param headingScaleFactor If headingFree is false, this weights the cost of changing our robot
     *     heading estimate against the tag corner reprojection error const.
     */
    public static final record ConstrainedSolvepnpParams(
            boolean headingFree, double headingScaleFactor) {}

    private AprilTagFieldLayout fieldTags;
    private TargetModel tagModel = TargetModel.kAprilTag36h11;
    private PoseStrategy primaryStrategy;
    private PoseStrategy multiTagFallbackStrategy = PoseStrategy.LOWEST_AMBIGUITY;
    private Transform3d robotToCamera;

    private Pose3d lastPose;
    private Pose3d referencePose;
    protected double poseCacheTimestampSeconds = -1;
    private final Set<Integer> reportedErrors = new HashSet<>();

    private final TimeInterpolatableBuffer<Rotation2d> headingBuffer =
            TimeInterpolatableBuffer.createBuffer(1.0);

    /**
     * Create a new PhotonPoseEstimator.
     *
     * @param fieldTags A WPILib {@link AprilTagFieldLayout} linking AprilTag IDs to Pose3d objects
     *     with respect to the FIRST field using the <a href=
     *     "https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/coordinate-systems.html#field-coordinate-system">Field
     *     Coordinate System</a>. Note that setting the origin of this layout object will affect the
     *     results from this class.
     * @param strategy The strategy it should use to determine the best pose.
     * @param robotToCamera Transform3d from the center of the robot to the camera mount position (ie,
     *     robot ➔ camera) in the <a href=
     *     "https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/coordinate-systems.html#robot-coordinate-system">Robot
     *     Coordinate System</a>.
     */
    public PhotonPoseEstimator(
            AprilTagFieldLayout fieldTags, PoseStrategy strategy, Transform3d robotToCamera) {
        this.fieldTags = fieldTags;
        this.primaryStrategy = strategy;
        this.robotToCamera = robotToCamera;

        HAL.report(tResourceType.kResourceType_PhotonPoseEstimator, InstanceCount);
        InstanceCount++;
    }

    /** Invalidates the pose cache. */
    private void invalidatePoseCache() {
        poseCacheTimestampSeconds = -1;
    }

    private void checkUpdate(Object oldObj, Object newObj) {
        if (oldObj != newObj && oldObj != null && !oldObj.equals(newObj)) {
            invalidatePoseCache();
        }
    }

    /**
     * Get the AprilTagFieldLayout being used by the PositionEstimator.
     *
     * <p>Note: Setting the origin of this layout will affect the results from this class.
     *
     * @return the AprilTagFieldLayout
     */
    public AprilTagFieldLayout getFieldTags() {
        return fieldTags;
    }

    /**
     * Set the AprilTagFieldLayout being used by the PositionEstimator.
     *
     * <p>Note: Setting the origin of this layout will affect the results from this class.
     *
     * @param fieldTags the AprilTagFieldLayout
     */
    public void setFieldTags(AprilTagFieldLayout fieldTags) {
        checkUpdate(this.fieldTags, fieldTags);
        this.fieldTags = fieldTags;
    }

    /**
     * Get the TargetModel representing the tags being detected. This is used for on-rio multitag.
     *
     * <p>By default, this is {@link TargetModel#kAprilTag36h11}.
     */
    public TargetModel getTagModel() {
        return tagModel;
    }

    /**
     * Set the TargetModel representing the tags being detected. This is used for on-rio multitag.
     *
     * @param tagModel E.g. {@link TargetModel#kAprilTag16h5}.
     */
    public void setTagModel(TargetModel tagModel) {
        this.tagModel = tagModel;
    }

    /**
     * Get the Position Estimation Strategy being used by the Position Estimator.
     *
     * @return the strategy
     */
    public PoseStrategy getPrimaryStrategy() {
        return primaryStrategy;
    }

    /**
     * Set the Position Estimation Strategy used by the Position Estimator.
     *
     * @param strategy the strategy to set
     */
    public void setPrimaryStrategy(PoseStrategy strategy) {
        checkUpdate(this.primaryStrategy, strategy);
        this.primaryStrategy = strategy;
    }

    /**
     * Set the Position Estimation Strategy used in multi-tag mode when only one tag can be seen. Must
     * NOT be MULTI_TAG_PNP
     *
     * @param strategy the strategy to set
     */
    public void setMultiTagFallbackStrategy(PoseStrategy strategy) {
        checkUpdate(this.multiTagFallbackStrategy, strategy);
        if (strategy == PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR
                || strategy == PoseStrategy.MULTI_TAG_PNP_ON_RIO) {
            DriverStation.reportWarning(
                    "Fallback cannot be set to MULTI_TAG_PNP! Setting to lowest ambiguity", false);
            strategy = PoseStrategy.LOWEST_AMBIGUITY;
        }
        this.multiTagFallbackStrategy = strategy;
    }

    /**
     * Return the reference position that is being used by the estimator.
     *
     * @return the referencePose
     */
    public Pose3d getReferencePose() {
        return referencePose;
    }

    /**
     * Update the stored reference pose for use when using the <b>CLOSEST_TO_REFERENCE_POSE</b>
     * strategy.
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose3d referencePose) {
        checkUpdate(this.referencePose, referencePose);
        this.referencePose = referencePose;
    }

    /**
     * Update the stored reference pose for use when using the <b>CLOSEST_TO_REFERENCE_POSE</b>
     * strategy.
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose2d referencePose) {
        setReferencePose(new Pose3d(referencePose));
    }

    /**
     * Update the stored last pose. Useful for setting the initial estimate when using the
     * <b>CLOSEST_TO_LAST_POSE</b> strategy.
     *
     * @param lastPose the lastPose to set
     */
    public void setLastPose(Pose3d lastPose) {
        this.lastPose = lastPose;
    }

    /**
     * Update the stored last pose. Useful for setting the initial estimate when using the
     * <b>CLOSEST_TO_LAST_POSE</b> strategy.
     *
     * @param lastPose the lastPose to set
     */
    public void setLastPose(Pose2d lastPose) {
        setLastPose(new Pose3d(lastPose));
    }

    /**
     * Add robot heading data to buffer. Must be called periodically for the
     * <b>PNP_DISTANCE_TRIG_SOLVE</b> strategy.
     *
     * @param timestampSeconds timestamp of the robot heading data.
     * @param heading Field-relative robot heading at given timestamp. Standard WPILIB field
     *     coordinates.
     */
    public void addHeadingData(double timestampSeconds, Rotation3d heading) {
        addHeadingData(timestampSeconds, heading.toRotation2d());
    }

    /**
     * Add robot heading data to buffer. Must be called periodically for the
     * <b>PNP_DISTANCE_TRIG_SOLVE</b> strategy.
     *
     * @param timestampSeconds timestamp of the robot heading data.
     * @param heading Field-relative robot heading at given timestamp. Standard WPILIB field
     *     coordinates.
     */
    public void addHeadingData(double timestampSeconds, Rotation2d heading) {
        headingBuffer.addSample(timestampSeconds, heading);
    }

    /**
     * @return The current transform from the center of the robot to the camera mount position
     */
    public Transform3d getRobotToCameraTransform() {
        return robotToCamera;
    }

    /**
     * Useful for pan and tilt mechanisms and such.
     *
     * @param robotToCamera The current transform from the center of the robot to the camera mount
     *     position
     */
    public void setRobotToCameraTransform(Transform3d robotToCamera) {
        this.robotToCamera = robotToCamera;
    }

    /**
     * Updates the estimated position of the robot, assuming no camera calibration is required for the
     * selected strategy. Returns empty if:
     *
     * <ul>
     *   <li>The timestamp of the provided pipeline result is the same as in the previous call to
     *       {@code update()}.
     *   <li>No targets were found in the pipeline results.
     * </ul>
     *
     * Will report a warning if strategy is multi-tag-on-rio because camera calibration data is not
     * provided in this overload.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return an {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> update(PhotonPipelineResult cameraResult) {
        return update(cameraResult, Optional.empty(), Optional.empty());
    }

    /**
     * Updates the estimated position of the robot. Returns empty if:
     *
     * <ul>
     *   <li>The timestamp of the provided pipeline result is the same as in the previous call to
     *       {@code update()}.
     *   <li>No targets were found in the pipeline results.
     *   <li>The strategy is CONSTRAINED_SOLVEPNP, but no constrainedPnpParams were provided (use the
     *       other function overload).
     * </ul>
     *
     * @param cameraMatrix Camera calibration data for multi-tag-on-rio strategy - can be empty
     *     otherwise
     * @param distCoeffs Camera calibration data for multi-tag-on-rio strategy - can be empty
     *     otherwise
     * @return an {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> update(
            PhotonPipelineResult cameraResult,
            Optional<Matrix<N3, N3>> cameraMatrix,
            Optional<Matrix<N8, N1>> distCoeffs) {
        return update(cameraResult, cameraMatrix, distCoeffs, Optional.empty());
    }

    /**
     * Updates the estimated position of the robot. Returns empty if:
     *
     * <ul>
     *   <li>The timestamp of the provided pipeline result is the same as in the previous call to
     *       {@code update()}.
     *   <li>No targets were found in the pipeline results.
     *   <li>The strategy is CONSTRAINED_SOLVEPNP, but the provided constrainedPnpParams are empty.
     * </ul>
     *
     * @param cameraMatrix Camera calibration data for multi-tag-on-rio strategy - can be empty
     *     otherwise
     * @param distCoeffs Camera calibration data for multi-tag-on-rio strategy - can be empty
     *     otherwise
     * @param constrainedPnpParams Constrained SolvePNP params, if needed.
     * @return an {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> update(
            PhotonPipelineResult cameraResult,
            Optional<Matrix<N3, N3>> cameraMatrix,
            Optional<Matrix<N8, N1>> distCoeffs,
            Optional<ConstrainedSolvepnpParams> constrainedPnpParams) {
        // Time in the past -- give up, since the following if expects times > 0
        if (cameraResult.getTimestampSeconds() < 0) {
            return Optional.empty();
        }

        // If the pose cache timestamp was set, and the result is from the same
        // timestamp, return an
        // empty result
        if (poseCacheTimestampSeconds > 0
                && Math.abs(poseCacheTimestampSeconds - cameraResult.getTimestampSeconds()) < 1e-6) {
            return Optional.empty();
        }

        // Remember the timestamp of the current result used
        poseCacheTimestampSeconds = cameraResult.getTimestampSeconds();

        // If no targets seen, trivial case -- return empty result
        if (!cameraResult.hasTargets()) {
            return Optional.empty();
        }

        return update(
                cameraResult, cameraMatrix, distCoeffs, constrainedPnpParams, this.primaryStrategy);
    }

    /**
     * Internal convenience method for using a fallback strategy for update(). This should only be
     * called after timestamp checks have been done by another update() overload.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @param strategy The pose strategy to use. Can't be CONSTRAINED_SOLVEPNP.
     * @return an {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    private Optional<EstimatedRobotPose> update(
            PhotonPipelineResult cameraResult, PoseStrategy strategy) {
        return update(cameraResult, Optional.empty(), Optional.empty(), Optional.empty(), strategy);
    }

    private Optional<EstimatedRobotPose> update(
            PhotonPipelineResult cameraResult,
            Optional<Matrix<N3, N3>> cameraMatrix,
            Optional<Matrix<N8, N1>> distCoeffs,
            Optional<ConstrainedSolvepnpParams> constrainedPnpParams,
            PoseStrategy strategy) {
        Optional<EstimatedRobotPose> estimatedPose =
                switch (strategy) {
                    case LOWEST_AMBIGUITY -> lowestAmbiguityStrategy(cameraResult);
                    case CLOSEST_TO_CAMERA_HEIGHT -> closestToCameraHeightStrategy(cameraResult);
                    case CLOSEST_TO_REFERENCE_POSE ->
                            closestToReferencePoseStrategy(cameraResult, referencePose);
                    case CLOSEST_TO_LAST_POSE -> {
                        setReferencePose(lastPose);
                        yield closestToReferencePoseStrategy(cameraResult, referencePose);
                    }
                    case AVERAGE_BEST_TARGETS -> averageBestTargetsStrategy(cameraResult);
                    case MULTI_TAG_PNP_ON_RIO ->
                            multiTagOnRioStrategy(cameraResult, cameraMatrix, distCoeffs);
                    case MULTI_TAG_PNP_ON_COPROCESSOR -> multiTagOnCoprocStrategy(cameraResult);
                    case PNP_DISTANCE_TRIG_SOLVE -> pnpDistanceTrigSolveStrategy(cameraResult);
                    case CONSTRAINED_SOLVEPNP ->
                            constrainedPnpStrategy(cameraResult, cameraMatrix, distCoeffs, constrainedPnpParams);
                };

        if (estimatedPose.isPresent()) {
            lastPose = estimatedPose.get().estimatedPose;
        }

        return estimatedPose;
    }

    private Optional<EstimatedRobotPose> pnpDistanceTrigSolveStrategy(PhotonPipelineResult result) {
        PhotonTrackedTarget bestTarget = result.getBestTarget();

        if (bestTarget == null) return Optional.empty();

        var headingSampleOpt = headingBuffer.getSample(result.getTimestampSeconds());
        if (headingSampleOpt.isEmpty()) {
            return Optional.empty();
        }
        Rotation2d headingSample = headingSampleOpt.get();

        Translation2d camToTagTranslation =
                new Translation3d(
                                bestTarget.getBestCameraToTarget().getTranslation().getNorm(),
                                new Rotation3d(
                                        0,
                                        -Math.toRadians(bestTarget.getPitch()),
                                        -Math.toRadians(bestTarget.getYaw())))
                        .rotateBy(robotToCamera.getRotation())
                        .toTranslation2d()
                        .rotateBy(headingSample);

        var tagPoseOpt = fieldTags.getTagPose(bestTarget.getFiducialId());
        if (tagPoseOpt.isEmpty()) {
            return Optional.empty();
        }
        var tagPose2d = tagPoseOpt.get().toPose2d();

        Translation2d fieldToCameraTranslation =
                tagPose2d.getTranslation().plus(camToTagTranslation.unaryMinus());

        Translation2d camToRobotTranslation =
                robotToCamera.getTranslation().toTranslation2d().unaryMinus().rotateBy(headingSample);

        Pose2d robotPose =
                new Pose2d(fieldToCameraTranslation.plus(camToRobotTranslation), headingSample);

        return Optional.of(
                new EstimatedRobotPose(
                        new Pose3d(robotPose),
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.PNP_DISTANCE_TRIG_SOLVE));
    }

    private Optional<EstimatedRobotPose> constrainedPnpStrategy(
            PhotonPipelineResult result,
            Optional<Matrix<N3, N3>> cameraMatrixOpt,
            Optional<Matrix<N8, N1>> distCoeffsOpt,
            Optional<ConstrainedSolvepnpParams> constrainedPnpParams) {
        boolean hasCalibData = cameraMatrixOpt.isPresent() && distCoeffsOpt.isPresent();
        // cannot run multitagPNP, use fallback strategy
        if (!hasCalibData) {
            return update(
                    result, cameraMatrixOpt, distCoeffsOpt, Optional.empty(), this.multiTagFallbackStrategy);
        }

        if (constrainedPnpParams.isEmpty()) {
            return Optional.empty();
        }

        // Need heading if heading fixed
        if (!constrainedPnpParams.get().headingFree
                && headingBuffer.getSample(result.getTimestampSeconds()).isEmpty()) {
            return update(
                    result, cameraMatrixOpt, distCoeffsOpt, Optional.empty(), this.multiTagFallbackStrategy);
        }

        Pose3d fieldToRobotSeed;

        // Attempt to use multi-tag to get a pose estimate seed
        if (result.getMultiTagResult().isPresent()) {
            fieldToRobotSeed =
                    Pose3d.kZero.plus(
                            result.getMultiTagResult().get().estimatedPose.best.plus(robotToCamera.inverse()));
        } else {
            // HACK - use fallback strategy to gimme a seed pose
            // TODO - make sure nested update doesn't break state
            var nestedUpdate =
                    update(
                            result,
                            cameraMatrixOpt,
                            distCoeffsOpt,
                            Optional.empty(),
                            this.multiTagFallbackStrategy);
            if (nestedUpdate.isEmpty()) {
                // best i can do is bail
                return Optional.empty();
            }
            fieldToRobotSeed = nestedUpdate.get().estimatedPose;
        }

        if (!constrainedPnpParams.get().headingFree) {
            // If heading fixed, force rotation component
            fieldToRobotSeed =
                    new Pose3d(
                            fieldToRobotSeed.getTranslation(),
                            new Rotation3d(headingBuffer.getSample(result.getTimestampSeconds()).get()));
        }

        var pnpResult =
                VisionEstimation.estimateRobotPoseConstrainedSolvepnp(
                        cameraMatrixOpt.get(),
                        distCoeffsOpt.get(),
                        result.getTargets(),
                        robotToCamera,
                        fieldToRobotSeed,
                        fieldTags,
                        tagModel,
                        constrainedPnpParams.get().headingFree,
                        headingBuffer.getSample(result.getTimestampSeconds()).get(),
                        constrainedPnpParams.get().headingScaleFactor);
        // try fallback strategy if solvePNP fails for some reason
        if (!pnpResult.isPresent())
            return update(
                    result, cameraMatrixOpt, distCoeffsOpt, Optional.empty(), this.multiTagFallbackStrategy);
        var best = Pose3d.kZero.plus(pnpResult.get().best); // field-to-robot

        return Optional.of(
                new EstimatedRobotPose(
                        best,
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.CONSTRAINED_SOLVEPNP));
    }

    private Optional<EstimatedRobotPose> multiTagOnCoprocStrategy(PhotonPipelineResult result) {
        if (result.getMultiTagResult().isEmpty()) {
            return update(result, this.multiTagFallbackStrategy);
        }

        var best_tf = result.getMultiTagResult().get().estimatedPose.best;
        var best =
                Pose3d.kZero
                        .plus(best_tf) // field-to-camera
                        .relativeTo(fieldTags.getOrigin())
                        .plus(robotToCamera.inverse()); // field-to-robot
        return Optional.of(
                new EstimatedRobotPose(
                        best,
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR));
    }

    private Optional<EstimatedRobotPose> multiTagOnRioStrategy(
            PhotonPipelineResult result,
            Optional<Matrix<N3, N3>> cameraMatrixOpt,
            Optional<Matrix<N8, N1>> distCoeffsOpt) {
        if (cameraMatrixOpt.isEmpty() || distCoeffsOpt.isEmpty()) {
            DriverStation.reportWarning(
                    "No camera calibration data provided for multi-tag-on-rio",
                    Thread.currentThread().getStackTrace());
            return update(result, this.multiTagFallbackStrategy);
        }

        if (result.getTargets().size() < 2) {
            return update(result, this.multiTagFallbackStrategy);
        }

        var pnpResult =
                VisionEstimation.estimateCamPosePNP(
                        cameraMatrixOpt.get(), distCoeffsOpt.get(), result.getTargets(), fieldTags, tagModel);
        // try fallback strategy if solvePNP fails for some reason
        if (!pnpResult.isPresent())
            return update(
                    result, cameraMatrixOpt, distCoeffsOpt, Optional.empty(), this.multiTagFallbackStrategy);

        var best =
                Pose3d.kZero
                        .plus(pnpResult.get().best) // field-to-camera
                        .plus(robotToCamera.inverse()); // field-to-robot

        return Optional.of(
                new EstimatedRobotPose(
                        best,
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.MULTI_TAG_PNP_ON_RIO));
    }

    /**
     * Return the estimated position of the robot with the lowest position ambiguity from a List of
     * pipeline results.
     *
     * @param result pipeline result
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> lowestAmbiguityStrategy(PhotonPipelineResult result) {
        PhotonTrackedTarget lowestAmbiguityTarget = null;

        double lowestAmbiguityScore = 10;

        for (PhotonTrackedTarget target : result.targets) {
            double targetPoseAmbiguity = target.getPoseAmbiguity();
            // Make sure the target is a Fiducial target.
            if (targetPoseAmbiguity != -1 && targetPoseAmbiguity < lowestAmbiguityScore) {
                lowestAmbiguityScore = targetPoseAmbiguity;
                lowestAmbiguityTarget = target;
            }
        }

        // Although there are confirmed to be targets, none of them may be fiducial
        // targets.
        if (lowestAmbiguityTarget == null) return Optional.empty();

        int targetFiducialId = lowestAmbiguityTarget.getFiducialId();

        Optional<Pose3d> targetPosition = fieldTags.getTagPose(targetFiducialId);

        if (targetPosition.isEmpty()) {
            reportFiducialPoseError(targetFiducialId);
            return Optional.empty();
        }

        return Optional.of(
                new EstimatedRobotPose(
                        targetPosition
                                .get()
                                .transformBy(lowestAmbiguityTarget.getBestCameraToTarget().inverse())
                                .transformBy(robotToCamera.inverse()),
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.LOWEST_AMBIGUITY));
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta height
     * difference between the estimated and actual height of the camera.
     *
     * @param result pipeline result
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> closestToCameraHeightStrategy(PhotonPipelineResult result) {
        double smallestHeightDifference = 10e9;
        EstimatedRobotPose closestHeightTarget = null;

        for (PhotonTrackedTarget target : result.targets) {
            int targetFiducialId = target.getFiducialId();

            // Don't report errors for non-fiducial targets. This could also be resolved by
            // adding -1 to
            // the initial HashSet.
            if (targetFiducialId == -1) continue;

            Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

            if (targetPosition.isEmpty()) {
                reportFiducialPoseError(target.getFiducialId());
                continue;
            }

            double alternateTransformDelta =
                    Math.abs(
                            robotToCamera.getZ()
                                    - targetPosition
                                            .get()
                                            .transformBy(target.getAlternateCameraToTarget().inverse())
                                            .getZ());
            double bestTransformDelta =
                    Math.abs(
                            robotToCamera.getZ()
                                    - targetPosition
                                            .get()
                                            .transformBy(target.getBestCameraToTarget().inverse())
                                            .getZ());

            if (alternateTransformDelta < smallestHeightDifference) {
                smallestHeightDifference = alternateTransformDelta;
                closestHeightTarget =
                        new EstimatedRobotPose(
                                targetPosition
                                        .get()
                                        .transformBy(target.getAlternateCameraToTarget().inverse())
                                        .transformBy(robotToCamera.inverse()),
                                result.getTimestampSeconds(),
                                result.getTargets(),
                                PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT);
            }

            if (bestTransformDelta < smallestHeightDifference) {
                smallestHeightDifference = bestTransformDelta;
                closestHeightTarget =
                        new EstimatedRobotPose(
                                targetPosition
                                        .get()
                                        .transformBy(target.getBestCameraToTarget().inverse())
                                        .transformBy(robotToCamera.inverse()),
                                result.getTimestampSeconds(),
                                result.getTargets(),
                                PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT);
            }
        }

        // Need to null check here in case none of the provided targets are fiducial.
        return Optional.ofNullable(closestHeightTarget);
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta in the vector
     * magnitude between it and the reference pose.
     *
     * @param result pipeline result
     * @param referencePose reference pose to check vector magnitude difference against.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> closestToReferencePoseStrategy(
            PhotonPipelineResult result, Pose3d referencePose) {
        if (referencePose == null) {
            DriverStation.reportError(
                    "[PhotonPoseEstimator] Tried to use reference pose strategy without setting the reference!",
                    false);
            return Optional.empty();
        }

        double smallestPoseDelta = 10e9;
        EstimatedRobotPose lowestDeltaPose = null;

        for (PhotonTrackedTarget target : result.targets) {
            int targetFiducialId = target.getFiducialId();

            // Don't report errors for non-fiducial targets. This could also be resolved by
            // adding -1 to
            // the initial HashSet.
            if (targetFiducialId == -1) continue;

            Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

            if (targetPosition.isEmpty()) {
                reportFiducialPoseError(targetFiducialId);
                continue;
            }

            Pose3d altTransformPosition =
                    targetPosition
                            .get()
                            .transformBy(target.getAlternateCameraToTarget().inverse())
                            .transformBy(robotToCamera.inverse());
            Pose3d bestTransformPosition =
                    targetPosition
                            .get()
                            .transformBy(target.getBestCameraToTarget().inverse())
                            .transformBy(robotToCamera.inverse());

            double altDifference = Math.abs(calculateDifference(referencePose, altTransformPosition));
            double bestDifference = Math.abs(calculateDifference(referencePose, bestTransformPosition));

            if (altDifference < smallestPoseDelta) {
                smallestPoseDelta = altDifference;
                lowestDeltaPose =
                        new EstimatedRobotPose(
                                altTransformPosition,
                                result.getTimestampSeconds(),
                                result.getTargets(),
                                PoseStrategy.CLOSEST_TO_REFERENCE_POSE);
            }
            if (bestDifference < smallestPoseDelta) {
                smallestPoseDelta = bestDifference;
                lowestDeltaPose =
                        new EstimatedRobotPose(
                                bestTransformPosition,
                                result.getTimestampSeconds(),
                                result.getTargets(),
                                PoseStrategy.CLOSEST_TO_REFERENCE_POSE);
            }
        }
        return Optional.ofNullable(lowestDeltaPose);
    }

    /**
     * Return the average of the best target poses using ambiguity as weight.
     *
     * @param result pipeline result
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> averageBestTargetsStrategy(PhotonPipelineResult result) {
        List<Pair<PhotonTrackedTarget, Pose3d>> estimatedRobotPoses = new ArrayList<>();
        double totalAmbiguity = 0;

        for (PhotonTrackedTarget target : result.targets) {
            int targetFiducialId = target.getFiducialId();

            // Don't report errors for non-fiducial targets. This could also be resolved by
            // adding -1 to
            // the initial HashSet.
            if (targetFiducialId == -1) continue;

            Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

            if (targetPosition.isEmpty()) {
                reportFiducialPoseError(targetFiducialId);
                continue;
            }

            double targetPoseAmbiguity = target.getPoseAmbiguity();

            // Pose ambiguity is 0, use that pose
            if (targetPoseAmbiguity == 0) {
                return Optional.of(
                        new EstimatedRobotPose(
                                targetPosition
                                        .get()
                                        .transformBy(target.getBestCameraToTarget().inverse())
                                        .transformBy(robotToCamera.inverse()),
                                result.getTimestampSeconds(),
                                result.getTargets(),
                                PoseStrategy.AVERAGE_BEST_TARGETS));
            }

            totalAmbiguity += 1.0 / target.getPoseAmbiguity();

            estimatedRobotPoses.add(
                    new Pair<>(
                            target,
                            targetPosition
                                    .get()
                                    .transformBy(target.getBestCameraToTarget().inverse())
                                    .transformBy(robotToCamera.inverse())));
        }

        // Take the average

        Translation3d transform = new Translation3d();
        Rotation3d rotation = new Rotation3d();

        if (estimatedRobotPoses.isEmpty()) return Optional.empty();

        for (Pair<PhotonTrackedTarget, Pose3d> pair : estimatedRobotPoses) {
            // Total ambiguity is non-zero confirmed because if it was zero, that pose was
            // returned.
            double weight = (1.0 / pair.getFirst().getPoseAmbiguity()) / totalAmbiguity;
            Pose3d estimatedPose = pair.getSecond();
            transform = transform.plus(estimatedPose.getTranslation().times(weight));
            rotation = rotation.plus(estimatedPose.getRotation().times(weight));
        }

        return Optional.of(
                new EstimatedRobotPose(
                        new Pose3d(transform, rotation),
                        result.getTimestampSeconds(),
                        result.getTargets(),
                        PoseStrategy.AVERAGE_BEST_TARGETS));
    }

    /**
     * Difference is defined as the vector magnitude between the two poses
     *
     * @return The absolute "difference" (>=0) between two Pose3ds.
     */
    private double calculateDifference(Pose3d x, Pose3d y) {
        return x.getTranslation().getDistance(y.getTranslation());
    }

    private void reportFiducialPoseError(int fiducialId) {
        if (!reportedErrors.contains(fiducialId)) {
            DriverStation.reportError(
                    "[PhotonPoseEstimator] Tried to get pose of unknown AprilTag: " + fiducialId, false);
            reportedErrors.add(fiducialId);
        }
    }
}

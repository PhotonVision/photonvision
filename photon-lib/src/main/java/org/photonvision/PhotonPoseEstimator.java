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
import edu.wpi.first.math.Vector;
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
import java.util.*;
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
    private static int InstanceCount = 1;

    private AprilTagFieldLayout fieldTags;
    private TargetModel tagModel = TargetModel.kAprilTag36h11;
    private Transform3d robotToCamera;

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
     * @param robotToCamera Transform3d from the center of the robot to the camera mount position (ie,
     *     robot ➔ camera) in the <a href=
     *     "https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/coordinate-systems.html#robot-coordinate-system">Robot
     *     Coordinate System</a>.
     */
    public PhotonPoseEstimator(AprilTagFieldLayout fieldTags, Transform3d robotToCamera) {
        this.fieldTags = fieldTags;
        this.robotToCamera = robotToCamera;

        HAL.report(tResourceType.kResourceType_PhotonPoseEstimator, InstanceCount);
        InstanceCount++;
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
     * Add robot heading data to buffer. Must be called periodically for the
     * <b>PNP_DISTANCE_TRIG_SOLVE</b> strategy.
     *
     * @param timestampSeconds Timestamp of the robot heading data.
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
     * @param timestampSeconds Timestamp of the robot heading data.
     * @param heading Field-relative robot heading at given timestamp. Standard WPILIB field
     *     coordinates.
     */
    public void addHeadingData(double timestampSeconds, Rotation2d heading) {
        headingBuffer.addSample(timestampSeconds, heading);
    }

    /**
     * Clears all heading data in the buffer, and adds a new seed. Useful for preventing estimates
     * from utilizing heading data provided prior to a pose or rotation reset.
     *
     * @param timestampSeconds Timestamp of the robot heading data.
     * @param heading Field-relative robot heading at given timestamp. Standard WPILIB field
     *     coordinates.
     */
    public void resetHeadingData(double timestampSeconds, Rotation3d heading) {
        headingBuffer.clear();
        addHeadingData(timestampSeconds, heading);
    }

    /**
     * Clears all heading data in the buffer, and adds a new seed. Useful for preventing estimates
     * from utilizing heading data provided prior to a pose or rotation reset.
     *
     * @param timestampSeconds Timestamp of the robot heading data.
     * @param heading Field-relative robot heading at given timestamp. Standard WPILIB field
     *     coordinates.
     */
    public void resetHeadingData(double timestampSeconds, Rotation2d heading) {
        headingBuffer.clear();
        addHeadingData(timestampSeconds, heading);
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
     * @param cameraResult The pipeline result from the camera
     * @return Whether or not pose estimation should be performed.
     */
    private boolean shouldEstimate(PhotonPipelineResult cameraResult) {
        // Time in the past -- give up, since the following if expects times > 0
        if (cameraResult.getTimestampSeconds() < 0) {
            return false;
        }

        // If no targets seen, trivial case -- return empty result
        return !cameraResult.hasTargets();
    }

    /**
     * Return the estimated position of the robot by using distance data from best visible tag to
     * compute a Pose. This runs on the RoboRIO in order to access the robot's yaw heading, and MUST
     * have addHeadingData called every frame so heading data is up-to-date.
     *
     * <p>Yields a Pose2d in estimatedRobotPose (0 for z, roll, pitch)
     *
     * <p>https://www.chiefdelphi.com/t/frc-6328-mechanical-advantage-2025-build-thread/477314/98
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimatePnpDistanceTrigSolvePose(
            PhotonPipelineResult cameraResult) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        PhotonTrackedTarget bestTarget = cameraResult.getBestTarget();

        if (bestTarget == null) return Optional.empty();

        var headingSampleOpt = headingBuffer.getSample(cameraResult.getTimestampSeconds());
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
                        new Pose3d(robotPose), cameraResult.getTimestampSeconds(), cameraResult.getTargets()));
    }

    /**
     * Return the estimated position of the robot by solving a constrained version of the
     * Perspective-n-Point problem with the robot's drivebase flat on the floor. This computation
     * takes place on the RoboRIO, and typically takes not more than 2ms. See {@link
     * org.photonvision.jni.ConstrainedSolvepnpJni} for tuning handles this strategy exposes.
     * Internally, the cost function is a sum-squared of pixel reprojection error + (optionally)
     * heading error heading scale factor. This strategy needs addHeadingData called every frame so
     * heading data is up-to-date.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @param cameraMatrix Camera intrinsics from camera calibration data
     * @param distCoeffs Distortion coefficients from camera calibration data
     * @param seedPose An initial guess at robot pose, refined via optimizaiton. Better guesses will
     *     converge faster.
     * @param headingFree If true, heading is completely free to vary. If false, heading excursions
     *     from the provided heading measurement will be penalized
     * @param headingScaleFactor If headingFree is false, this weights the cost of changing our robot
     *     heading estimate against the tag corner reprojection error const.
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateConstrainedPnpPose(
            PhotonPipelineResult cameraResult,
            Matrix<N3, N3> cameraMatrix,
            Vector<N8> distCoeffs,
            Pose3d seedPose,
            boolean headingFree,
            double headingScaleFactor) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        var pnpResult =
                VisionEstimation.estimateRobotPoseConstrainedSolvepnp(
                        cameraMatrix,
                        distCoeffs,
                        cameraResult.getTargets(),
                        robotToCamera,
                        seedPose,
                        fieldTags,
                        tagModel,
                        headingFree,
                        headingBuffer.getSample(cameraResult.getTimestampSeconds()).get(),
                        headingScaleFactor);
        if (!pnpResult.isPresent()) return Optional.empty();
        var best = Pose3d.kZero.plus(pnpResult.get().best); // field-to-robot

        return Optional.of(
                new EstimatedRobotPose(
                        best, cameraResult.getTimestampSeconds(), cameraResult.getTargets()));
    }

    /**
     * Return the estimated position of the robot by using all visible tags to compute a single pose
     * estimate on coprocessor. This option needs to be enabled on the PhotonVision web UI as well.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateCoprocMultiTagPose(
            PhotonPipelineResult cameraResult) {
        if (cameraResult.getMultiTagResult().isEmpty() || !shouldEstimate(cameraResult)) {
            return Optional.empty();
        }

        var best_tf = cameraResult.getMultiTagResult().get().estimatedPose.best;
        var best =
                Pose3d.kZero
                        .plus(best_tf) // field-to-camera
                        .relativeTo(fieldTags.getOrigin())
                        .plus(robotToCamera.inverse()); // field-to-robot
        return Optional.of(
                new EstimatedRobotPose(
                        best, cameraResult.getTimestampSeconds(), cameraResult.getTargets()));
    }

    /**
     * Return the estimated position of the robot by using all visible tags to compute a single pose
     * estimate on the RoboRIO. This can take a lot of time due to the RIO's weak computing power.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @param cameraMatrix Camera intrinsics from camera calibration data
     * @param distCoeffs Distortion coefficients from camera calibration data
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateRioMultiTagPose(
            PhotonPipelineResult cameraResult, Matrix<N3, N3> cameraMatrix, Matrix<N8, N1> distCoeffs) {
        if (cameraResult.getTargets().size() < 2 || !shouldEstimate(cameraResult)) {
            return Optional.empty();
        }

        var pnpResult =
                VisionEstimation.estimateCamPosePNP(
                        cameraMatrix, distCoeffs, cameraResult.getTargets(), fieldTags, tagModel);
        if (!pnpResult.isPresent()) return Optional.empty();

        var best =
                Pose3d.kZero
                        .plus(pnpResult.get().best) // field-to-camera
                        .plus(robotToCamera.inverse()); // field-to-robot

        return Optional.of(
                new EstimatedRobotPose(
                        best, cameraResult.getTimestampSeconds(), cameraResult.getTargets()));
    }

    /**
     * Return the estimated position of the robot with the lowest position ambiguity from a pipeline
     * result.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateLowestAmbiguityPose(
            PhotonPipelineResult cameraResult) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        PhotonTrackedTarget lowestAmbiguityTarget = null;

        double lowestAmbiguityScore = 10;

        for (PhotonTrackedTarget target : cameraResult.targets) {
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
                        cameraResult.getTimestampSeconds(),
                        cameraResult.getTargets()));
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta height
     * difference between the estimated and actual height of the camera.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateClosestToCameraHeightPose(
            PhotonPipelineResult cameraResult) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        double smallestHeightDifference = 10e9;
        EstimatedRobotPose closestHeightTarget = null;

        for (PhotonTrackedTarget target : cameraResult.targets) {
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
                                cameraResult.getTimestampSeconds(),
                                cameraResult.getTargets());
            }

            if (bestTransformDelta < smallestHeightDifference) {
                smallestHeightDifference = bestTransformDelta;
                closestHeightTarget =
                        new EstimatedRobotPose(
                                targetPosition
                                        .get()
                                        .transformBy(target.getBestCameraToTarget().inverse())
                                        .transformBy(robotToCamera.inverse()),
                                cameraResult.getTimestampSeconds(),
                                cameraResult.getTargets());
            }
        }

        // Need to null check here in case none of the provided targets are fiducial.
        return Optional.ofNullable(closestHeightTarget);
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta in the vector
     * magnitude between it and the reference pose.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @param referencePose reference pose to check vector magnitude difference against.
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateClosestToReferencePose(
            PhotonPipelineResult cameraResult, Pose3d referencePose) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        if (referencePose == null) {
            DriverStation.reportError(
                    "[PhotonPoseEstimator] Tried to use reference pose strategy without setting the reference!",
                    false);
            return Optional.empty();
        }

        double smallestPoseDelta = 10e9;
        EstimatedRobotPose lowestDeltaPose = null;

        for (PhotonTrackedTarget target : cameraResult.targets) {
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
                                cameraResult.getTimestampSeconds(),
                                cameraResult.getTargets());
            }
            if (bestDifference < smallestPoseDelta) {
                smallestPoseDelta = bestDifference;
                lowestDeltaPose =
                        new EstimatedRobotPose(
                                bestTransformPosition,
                                cameraResult.getTimestampSeconds(),
                                cameraResult.getTargets());
            }
        }
        return Optional.ofNullable(lowestDeltaPose);
    }

    /**
     * Return the average of the best target poses using ambiguity as weight.
     *
     * @param cameraResult The latest pipeline result from the camera
     * @return An {@link EstimatedRobotPose} with an estimated pose, timestamp, and targets used to
     *     create the estimate.
     */
    public Optional<EstimatedRobotPose> estimateAverageBestTargetsPose(
            PhotonPipelineResult cameraResult) {
        if (!shouldEstimate(cameraResult)) {
            return Optional.empty();
        }
        List<Pair<PhotonTrackedTarget, Pose3d>> estimatedRobotPoses = new ArrayList<>();
        double totalAmbiguity = 0;

        for (PhotonTrackedTarget target : cameraResult.targets) {
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
                                cameraResult.getTimestampSeconds(),
                                cameraResult.getTargets()));
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
                        cameraResult.getTimestampSeconds(),
                        cameraResult.getTargets()));
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

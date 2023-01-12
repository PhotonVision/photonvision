/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public class RobotPoseEstimator {
    /** Position estimation strategies that can be used by the {@link RobotPoseEstimator} class. */
    public enum PoseStrategy {
        /** Choose the Pose with the lowest ambiguity. */
        LOWEST_AMBIGUITY,

        /** Choose the Pose which is closest to the camera height. */
        CLOSEST_TO_CAMERA_HEIGHT,

        /** Choose the Pose which is closest to a set Reference position. */
        CLOSEST_TO_REFERENCE_POSE,

        /** Choose the Pose which is closest to the last pose calculated */
        CLOSEST_TO_LAST_POSE,

        /** Choose the Pose with the lowest ambiguity. */
        AVERAGE_BEST_TARGETS
    }

    private AprilTagFieldLayout fieldTags;
    private PoseStrategy strategy;
    private final List<Pair<PhotonCamera, Transform3d>> cameras;

    private Pose3d lastPose;
    private Pose3d referencePose;
    private final Set<Integer> reportedErrors = new HashSet<>();

    /**
     * Create a new RobotPoseEstimator.
     *
     * @param fieldTags A WPILib {@link AprilTagFieldLayout} linking AprilTag IDs to Pose3ds with
     *     respect to the FIRST field.
     * @param strategy The strategy it should use to determine the best pose.
     * @param cameras An ArrayList of Pairs of PhotonCameras and their respective Transform3ds from
     *     the center of the robot to the camera mount positions (ie, robot ➔ camera).
     */
    public RobotPoseEstimator(
            AprilTagFieldLayout fieldTags,
            PoseStrategy strategy,
            List<Pair<PhotonCamera, Transform3d>> cameras) {
        this.fieldTags = fieldTags;
        this.strategy = strategy;
        this.cameras = cameras;
    }

    /**
     * Get the AprilTagFieldLayout being used by the PositionEstimator.
     *
     * @return the AprilTagFieldLayout
     */
    public AprilTagFieldLayout getFieldTags() {
        return fieldTags;
    }

    /**
     * Set the AprilTagFieldLayout being used by the PositionEstimator.
     *
     * @param fieldTags the AprilTagFieldLayout
     */
    public void setFieldTags(AprilTagFieldLayout fieldTags) {
        this.fieldTags = fieldTags;
    }

    /**
     * Get the Position Estimation Strategy being used by the Position Estimator.
     *
     * @return the strategy
     */
    public PoseStrategy getStrategy() {
        return strategy;
    }

    /**
     * Set the Position Estimation Strategy used by the Position Estimator.
     *
     * @param strategy the strategy to set
     */
    public void setStrategy(PoseStrategy strategy) {
        this.strategy = strategy;
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
        this.referencePose = referencePose;
    }

    /**
     * Update the stored reference pose for use when using the <b>CLOSEST_TO_REFERENCE_POSE</b>
     * strategy.
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose2d referencePose) {
        this.referencePose = new Pose3d(referencePose);
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
        this.lastPose = new Pose3d(lastPose);
    }

    /**
     * Poll data from the configured cameras and update the estimated position of the robot. Returns
     * empty if there are no cameras set or no targets were found from the cameras.
     *
     * @return an EstimatedRobotPose with an estimated pose, and information about the camera(s) and
     *     pipeline results used to create the estimate
     */
    public Optional<EstimatedRobotPose> update() {
        if (cameras.isEmpty()) {
            DriverStation.reportError("[RobotPoseEstimator] Missing any camera!", false);
            return Optional.empty();
        }

        ArrayList<CameraResult> cameraResults = new ArrayList<>(cameras.size());
        boolean hasTargets = false;

        for (Pair<PhotonCamera, Transform3d> p : cameras) {
            PhotonPipelineResult pipelineResults = p.getFirst().getLatestResult();

            if (pipelineResults.hasTargets()) hasTargets = true;

            cameraResults.add(new CameraResult(p.getFirst(), p.getSecond(), pipelineResults));
        }

        if (!hasTargets) {
            return Optional.empty();
        }

        Optional<EstimatedRobotPose> estimatedPose;
        switch (strategy) {
            case LOWEST_AMBIGUITY:
                estimatedPose = lowestAmbiguityStrategy(cameraResults);
                break;
            case CLOSEST_TO_CAMERA_HEIGHT:
                estimatedPose = closestToCameraHeightStrategy(cameraResults);
                break;
            case CLOSEST_TO_LAST_POSE:
                setReferencePose(lastPose);
            case CLOSEST_TO_REFERENCE_POSE:
                estimatedPose = closestToReferencePoseStrategy(cameraResults, referencePose);
                break;
            case AVERAGE_BEST_TARGETS:
                estimatedPose = averageBestTargetsStrategy(cameraResults);
                break;
            default:
                DriverStation.reportError(
                        "[RobotPoseEstimator] Unknown Position Estimation Strategy!", false);
                return Optional.empty();
        }

        if (estimatedPose.isEmpty()) {
            lastPose = null;
            return Optional.empty();
        }

        return estimatedPose;
    }

    /**
     * Return the estimated position of the robot with the lowest position ambiguity from a List of
     * pipeline results.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in
     *     the RCS from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> lowestAmbiguityStrategy(List<CameraResult> results) {
        Pair<PhotonTrackedTarget, Transform3d> lowestAmbiguityTarget = null;
        CameraResult lowestAmbiguityCameraResult = null;

        double lowestAmbiguityScore = 10;

        for (CameraResult result : results) {
            for (PhotonTrackedTarget target : result.pipelineResult.targets) {
                double targetPoseAmbiguity = target.getPoseAmbiguity();
                // Make sure the target is a Fiducial target.
                if (targetPoseAmbiguity != -1 && targetPoseAmbiguity < lowestAmbiguityScore) {
                    lowestAmbiguityScore = targetPoseAmbiguity;
                    lowestAmbiguityTarget = Pair.of(target, result.cameraPose);
                    lowestAmbiguityCameraResult = result;
                }
            }
        }

        // Although there are confirmed to be targets, none of them may be fiducial targets.
        if (lowestAmbiguityTarget == null) return Optional.empty();

        int targetFiducialId = lowestAmbiguityTarget.getFirst().getFiducialId();

        Optional<Pose3d> targetPosition = fieldTags.getTagPose(targetFiducialId);

        if (targetPosition.isEmpty()) {
            reportFiducialPoseError(targetFiducialId);
            return Optional.empty();
        }

        return Optional.of(
                new EstimatedRobotPose(
                        targetPosition
                                .get()
                                .transformBy(lowestAmbiguityTarget.getFirst().getBestCameraToTarget().inverse())
                                .transformBy(lowestAmbiguityTarget.getSecond().inverse()),
                        lowestAmbiguityCameraResult.camera,
                        lowestAmbiguityCameraResult.pipelineResult));
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta height
     * difference between the estimated and actual height of the camera.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in
     *     the RCS from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> closestToCameraHeightStrategy(List<CameraResult> results) {
        double smallestHeightDifference = 10e9;
        EstimatedRobotPose closestHeightTarget = null;

        for (CameraResult result : results) {
            for (PhotonTrackedTarget target : result.pipelineResult.targets) {
                int targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to
                // the initial HashSet.
                if (targetFiducialId == -1) continue;

                Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

                if (targetPosition.isEmpty()) {
                    reportFiducialPoseError(target.getFiducialId());
                    continue;
                }

                double alternateTransformDelta =
                        Math.abs(
                                result.cameraPose.getZ()
                                        - targetPosition
                                                .get()
                                                .transformBy(target.getAlternateCameraToTarget().inverse())
                                                .getZ());
                double bestTransformDelta =
                        Math.abs(
                                result.cameraPose.getZ()
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
                                            .transformBy(result.cameraPose.inverse()),
                                    result.camera,
                                    result.pipelineResult);
                }

                if (bestTransformDelta < smallestHeightDifference) {
                    smallestHeightDifference = bestTransformDelta;
                    closestHeightTarget =
                            new EstimatedRobotPose(
                                    targetPosition
                                            .get()
                                            .transformBy(target.getBestCameraToTarget().inverse())
                                            .transformBy(result.cameraPose.inverse()),
                                    result.camera,
                                    result.pipelineResult);
                }
            }
        }

        // Need to null check here in case none of the provided targets are fiducial.
        return Optional.ofNullable(closestHeightTarget);
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta in the vector
     * magnitude between it and the reference pose.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in
     *     the RCS from the robot origin to the camera.
     * @param referencePose reference pose to check vector magnitude difference against.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> closestToReferencePoseStrategy(
            List<CameraResult> results, Pose3d referencePose) {
        if (referencePose == null) {
            DriverStation.reportError(
                    "[RobotPoseEstimator] Tried to use reference pose strategy without setting the reference!",
                    false);
            return Optional.empty();
        }

        double smallestPoseDelta = 10e9;
        EstimatedRobotPose lowestDeltaPose = null;

        for (CameraResult result : results) {
            for (PhotonTrackedTarget target : result.pipelineResult.targets) {
                int targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to
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
                                .transformBy(result.cameraPose.inverse());
                Pose3d bestTransformPosition =
                        targetPosition
                                .get()
                                .transformBy(target.getBestCameraToTarget().inverse())
                                .transformBy(result.cameraPose.inverse());

                double altDifference = Math.abs(calculateDifference(referencePose, altTransformPosition));
                double bestDifference = Math.abs(calculateDifference(referencePose, bestTransformPosition));

                if (altDifference < smallestPoseDelta) {
                    smallestPoseDelta = altDifference;
                    lowestDeltaPose =
                            new EstimatedRobotPose(altTransformPosition, result.camera, result.pipelineResult);
                }
                if (bestDifference < smallestPoseDelta) {
                    smallestPoseDelta = bestDifference;
                    lowestDeltaPose =
                            new EstimatedRobotPose(bestTransformPosition, result.camera, result.pipelineResult);
                }
            }
        }
        return Optional.ofNullable(lowestDeltaPose);
    }

    /**
     * Return the average of the best target poses using ambiguity as weight.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in
     *     the RCS from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this
     *     estimation.
     */
    private Optional<EstimatedRobotPose> averageBestTargetsStrategy(List<CameraResult> results) {
        List<Pair<PhotonTrackedTarget, EstimatedRobotPose>> estimatedRobotPoses = new ArrayList<>();
        double totalAmbiguity = 0;

        for (CameraResult result : results) {
            for (PhotonTrackedTarget target : result.pipelineResult.targets) {
                int targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to
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
                                            .transformBy(result.cameraPose.inverse()),
                                    result.camera,
                                    result.pipelineResult));
                }

                totalAmbiguity += 1.0 / target.getPoseAmbiguity();

                estimatedRobotPoses.add(
                        new Pair<>(
                                target,
                                new EstimatedRobotPose(
                                        targetPosition
                                                .get()
                                                .transformBy(target.getBestCameraToTarget().inverse())
                                                .transformBy(result.cameraPose.inverse()),
                                        result.camera,
                                        result.pipelineResult)));
            }
        }

        // Take the average

        Translation3d transform = new Translation3d();
        Rotation3d rotation = new Rotation3d();

        if (estimatedRobotPoses.isEmpty()) return Optional.empty();

        for (Pair<PhotonTrackedTarget, EstimatedRobotPose> pair : estimatedRobotPoses) {
            // Total ambiguity is non-zero confirmed because if it was zero, that pose was returned.
            double weight = (1.0 / pair.getFirst().getPoseAmbiguity()) / totalAmbiguity;
            Pose3d estimatedPose = pair.getSecond().estimatedPose;
            transform = transform.plus(estimatedPose.getTranslation().times(weight));
            rotation = rotation.plus(estimatedPose.getRotation().times(weight));
        }

        List<EstimatedRobotPose.CameraPipelineResult> cameraPipelineResults =
                estimatedRobotPoses.stream()
                        .flatMap(e -> e.getSecond().cameraPipelineResults.stream())
                        .toList();

        return Optional.of(
                new EstimatedRobotPose(new Pose3d(transform, rotation), cameraPipelineResults));
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
                    "[RobotPoseEstimator] Tried to get pose of unknown April Tag: " + fiducialId, false);
            reportedErrors.add(fiducialId);
        }
    }

    private static class CameraResult {
        final PhotonCamera camera;
        final Transform3d cameraPose;
        final PhotonPipelineResult pipelineResult;

        public CameraResult(
                PhotonCamera camera, Transform3d cameraPose, PhotonPipelineResult pipelineResult) {
            this.camera = camera;
            this.cameraPose = cameraPose;
            this.pipelineResult = pipelineResult;
        }
    }
}

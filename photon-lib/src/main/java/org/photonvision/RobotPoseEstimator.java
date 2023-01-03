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
    /**
     * Position estimation strategies that can be used by the {@link RobotPoseEstimator} class.
     */
    public enum PoseStrategy {
        /**
         * Choose the Pose with the lowest ambiguity.
         */
        LOWEST_AMBIGUITY,

        /**
         * Choose the Pose which is closest to the camera height.
         */
        CLOSEST_TO_CAMERA_HEIGHT,

        /**
         * Choose the Pose which is closest to a set Reference position.
         */
        CLOSEST_TO_REFERENCE_POSE,

        /**
         * Choose the Pose which is closest to the last pose
         *  calculated
         */
        CLOSEST_TO_LAST_POSE,

        /**
         * Choose the Pose with the lowest ambiguity.
         */
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
     * <p>Example: {@code <code> <p> Map<Integer, Pose3d> map = new HashMap<>(); <p> map.put(1, new
     * Pose3d(1.0, 2.0, 3.0, new Rotation3d())); // Tag ID 1 is at (1.0,2.0,3.0) </code> }
     *
     * @param fieldTags A AprilTagFieldLayout linking AprilTag IDs to Pose3ds with respect to the
     *     FIRST field.
     * @param strategy The strategy it should use to determine the best pose.
     * @param cameras An ArrayList of Pairs of PhotonCameras and their respective Transform3ds from
     *     the center of the robot to the cameras.
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
     * Update the stored reference pose for use when using the <b>CLOSEST_TO_REFERENCE_POSE</b> strategy.
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose3d referencePose) {
        this.referencePose = referencePose;
    }

    /**
     * Update the stored reference pose for use when using the <b>CLOSEST_TO_REFERENCE_POSE</b> strategy.
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose2d referencePose) {
        this.referencePose = new Pose3d(referencePose);
    }

    /**
     * Update the stored last pose. Useful for setting the initial estimate when using the <b>CLOSEST_TO_LAST_POSE</b> strategy.
     *
     * @param lastPose the lastPose to set
     */
    public void setLastPose(Pose3d lastPose) {
        this.lastPose = lastPose;
    }

    /**
     * Update the stored last pose. Useful for setting the initial estimate when using the <b>CLOSEST_TO_LAST_POSE</b> strategy.
     *
     * @param lastPose the lastPose to set
     */
    public void setLastPose(Pose2d lastPose) {
        this.lastPose = new Pose3d(lastPose);
    }

    /**
     * Poll data from the configured cameras and update the estimated position of the robot. Returns empty if there are no cameras set or
     * no targets were found from the cameras.
     *
     * @return a Pair of the estimated Position of the robot in the form of a Pose3d object and the timestamp of that
     * position from the camera in seconds that is comparable to the FPGA hardware clock (in seconds).
     */
    public Optional<Pair<Pose3d, Double>> update() {
        if(cameras.isEmpty()) {
            DriverStation.reportError("[RobotPoseEstimator] Missing any camera!", false);
            return Optional.empty();
        }

        ArrayList<Pair<PhotonPipelineResult, Transform3d>> cameraResults = new ArrayList<>(cameras.size());
        boolean hasTargets = false;

        for(Pair<PhotonCamera, Transform3d> p : cameras) {
            PhotonPipelineResult pipelineResults = p.getFirst().getLatestResult();

            if(pipelineResults.hasTargets())
                hasTargets = true;

            cameraResults.add(Pair.of(pipelineResults, p.getSecond()));
        }

        if(!hasTargets) {
            return Optional.empty();
        }

        Optional<Pair<Pose3d, Double>> estimatedPose;
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
                estimatedPose = Optional.empty();
                break;
            default:
                DriverStation.reportError("[RobotPoseEstimator] Unknown Position Estimation Strategy!", false);
                return Optional.empty();
        }

        if(estimatedPose.isEmpty()) {
            lastPose = null;
            return Optional.empty();
        }

        return estimatedPose;
    }

    /**
     * Return the estimated position of the robot with the lowest position ambiguity from a List of pipeline results.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in the RCS
     *                from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this estimation.
     */
    private Optional<Pair<Pose3d, Double>> lowestAmbiguityStrategy(List<Pair<PhotonPipelineResult, Transform3d>> results) {
        Pair<PhotonTrackedTarget, Transform3d> lowestAmbiguityTarget = null;
        double lowestAmbiguityTargetTimestamp = 0;

        double lowestAmbiguityScore = 10;

        for(Pair<PhotonPipelineResult, Transform3d> result : results) {
            for(PhotonTrackedTarget target : result.getFirst().targets) {
                double targetPoseAmbiguity = target.getPoseAmbiguity();

                // Make sure the target is a Fiducial target.
                if(targetPoseAmbiguity != -1 && targetPoseAmbiguity < lowestAmbiguityScore) {
                    lowestAmbiguityTarget = Pair.of(target, result.getSecond());
                    lowestAmbiguityTargetTimestamp = result.getFirst().getTimestampSeconds();
                }
            }
        }

        // Although there are confirmed to be targets, none of them may be fiducial targets.
        if(lowestAmbiguityTarget == null)
            return Optional.empty();

        Optional<Pose3d> targetPosition = fieldTags.getTagPose(lowestAmbiguityTarget.getFirst().getFiducialId());

        if(targetPosition.isEmpty()) {
            int targetFiducialId = lowestAmbiguityTarget.getFirst().getFiducialId();
            if (!reportedErrors.contains(targetFiducialId)) {
                DriverStation.reportError(
                        "[RobotPoseEstimator] Tried to get pose of unknown April Tag: " + targetFiducialId,
                        false);
                reportedErrors.add(targetFiducialId);
            }

            return Optional.empty();
        }

        return Optional.of(
                Pair.of(
                        targetPosition.get()
                                .transformBy(lowestAmbiguityTarget.getFirst().getBestCameraToTarget().inverse())
                                .transformBy(lowestAmbiguityTarget.getSecond().inverse()),
                        lowestAmbiguityTargetTimestamp
                )
        );
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta height difference between the estimated and actual height of the camera.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in the RCS
     *                from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this estimation.
     */
    private Optional<Pair<Pose3d, Double>> closestToCameraHeightStrategy(List<Pair<PhotonPipelineResult, Transform3d>> results) {
        double smallestHeightDifference = 10e9;
        Pair<Pose3d, Double> closestHeightTarget = null;

        for(Pair<PhotonPipelineResult, Transform3d> result : results) {
            for(PhotonTrackedTarget target : result.getFirst().targets) {
                double targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to the initial HashSet.
                if(targetFiducialId == -1)
                    continue;

                Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

                if(targetPosition.isEmpty()) {
                    if (!reportedErrors.contains(target.getFiducialId())) {
                        DriverStation.reportWarning(
                                "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                        + target.getFiducialId(),
                                false);
                        reportedErrors.add(target.getFiducialId());
                    }
                }

                double alternateTransformDelta = Math.abs(
                        result.getSecond().getZ()
                                - targetPosition.get().transformBy(target.getAlternateCameraToTarget().inverse()).getZ()
                );
                double bestTransformDelta = Math.abs(
                        result.getSecond().getZ()
                                - targetPosition.get().transformBy(target.getBestCameraToTarget().inverse()).getZ()
                );

                if(alternateTransformDelta < smallestHeightDifference) {
                    smallestHeightDifference = alternateTransformDelta;
                    closestHeightTarget = Pair.of(
                            targetPosition.get()
                                    .transformBy(target.getAlternateCameraToTarget().inverse())
                                    .transformBy(result.getSecond().inverse()),
                            result.getFirst().getTimestampSeconds()
                    );
                }

                if(bestTransformDelta < smallestHeightDifference) {
                    smallestHeightDifference = bestTransformDelta;
                    closestHeightTarget = Pair.of(
                            targetPosition.get()
                                    .transformBy(target.getBestCameraToTarget().inverse())
                                    .transformBy(result.getSecond().inverse()),
                            result.getFirst().getTimestampSeconds()
                    );
                }


            }
        }

        // Need to null check here in case none of the provided targets are fiducial.
        if(closestHeightTarget == null)
            return Optional.empty();

        return Optional.of(closestHeightTarget);
    }

    /**
     * Return the estimated position of the robot using the target with the lowest delta in the vector magnitude between it and the reference pose.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in the RCS
     *                from the robot origin to the camera.
     * @param referencePose reference pose to check vector magnitude difference against.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this estimation.
     */
    private Optional<Pair<Pose3d, Double>> closestToReferencePoseStrategy(List<Pair<PhotonPipelineResult, Transform3d>> results, Pose3d referencePose) {
        if (referencePose == null) {
            DriverStation.reportError(
                    "[RobotPoseEstimator] Tried to use reference pose strategy without setting the reference!",
                    false);
            return Optional.empty();
        }

        double smallestPoseDelta = 10e9;
        Pair<Pose3d, Double> lowestDeltaPose = null;

        for(Pair<PhotonPipelineResult, Transform3d> result : results) {
            for(PhotonTrackedTarget target : result.getFirst().targets) {
                double targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to the initial HashSet.
                if(targetFiducialId == -1)
                    continue;

                Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

                if(targetPosition.isEmpty()) {
                    if (!reportedErrors.contains(target.getFiducialId())) {
                        DriverStation.reportWarning(
                                "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                        + target.getFiducialId(),
                                false);
                        reportedErrors.add(target.getFiducialId());
                    }
                }

                Pose3d altTransformPosition = targetPosition.get()
                        .transformBy(target.getAlternateCameraToTarget().inverse())
                        .transformBy(result.getSecond().inverse());
                Pose3d bestTransformPosition = targetPosition.get()
                        .transformBy(target.getBestCameraToTarget().inverse())
                        .transformBy(result.getSecond().inverse());

                double altDifference = Math.abs(calculateDifference(referencePose, altTransformPosition));
                double bestDifference = Math.abs(calculateDifference(referencePose, bestTransformPosition));

                if(altDifference < smallestPoseDelta) {
                    smallestPoseDelta = altDifference;
                    lowestDeltaPose = Pair.of(altTransformPosition, result.getFirst().getTimestampSeconds());
                }
                if(bestDifference < smallestPoseDelta) {
                    smallestPoseDelta = bestDifference;
                    lowestDeltaPose = Pair.of(bestTransformPosition, result.getFirst().getTimestampSeconds());
                }
            }
        }

        // Need to null check here in case none of the provided targets are fiducial.
        if(lowestDeltaPose == null)
            return Optional.empty();

        return Optional.of(lowestDeltaPose);
    }

    /**
     * Return the average of the best target poses using ambiguity as weight.
     *
     * @param results non-empty list of pipeline results and their camera's position transformation in the RCS
     *                from the robot origin to the camera.
     * @return the estimated position of the robot in the FCS and the estimated timestamp of this estimation.
     */
    private Optional<Pair<Pose3d, Double>> averageBestTargetsStrategy(List<Pair<PhotonPipelineResult, Transform3d>> results) {
        // List<Pair<RobotPose, Pair<PoseAmbiguity, StateTimestamp>>>
        List<Pair<Pose3d, Pair<Double, Double>>> targetData = new ArrayList<>();
        double totalAmbiguity = 0;

        for(Pair<PhotonPipelineResult, Transform3d> result : results) {
            for(PhotonTrackedTarget target : result.getFirst().targets) {
                double targetFiducialId = target.getFiducialId();

                // Don't report errors for non-fiducial targets. This could also be resolved by adding -1 to the initial HashSet.
                if(targetFiducialId == -1)
                    continue;

                Optional<Pose3d> targetPosition = fieldTags.getTagPose(target.getFiducialId());

                if(targetPosition.isEmpty()) {
                    if (!reportedErrors.contains(target.getFiducialId())) {
                        DriverStation.reportWarning(
                                "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                        + target.getFiducialId(),
                                false);
                        reportedErrors.add(target.getFiducialId());
                    }
                }

                double targetPoseAmbiguity = target.getPoseAmbiguity();
                double targetPoseTimestamp = result.getFirst().getTimestampSeconds();

                // Pose ambiguity is 0, use that pose
                if(targetPoseAmbiguity == 0) {
                    return Optional.of(
                            Pair.of(
                                    targetPosition.get()
                                            .transformBy(target.getBestCameraToTarget().inverse())
                                            .transformBy(result.getSecond().inverse()),
                                    targetPoseTimestamp
                            )
                    );
                }

                totalAmbiguity += 1.0 / target.getPoseAmbiguity();

                targetData.add(
                        Pair.of(
                                targetPosition.get()
                                        .transformBy(target.getBestCameraToTarget().inverse())
                                        .transformBy(result.getSecond().inverse()),
                                Pair.of(targetPoseAmbiguity, targetPoseTimestamp)
                        )
                );
            }
        }

        // Take the average

        Translation3d transform = new Translation3d();
        Rotation3d rotation = new Rotation3d();

        if(targetData.size() == 0)
            return Optional.empty();

        double timestampSum = 0;

        for(Pair<Pose3d, Pair<Double, Double>> pair : targetData) {
            // Total ambiguity is non-zero confirmed because if it was zero, that pose was returned.
            double weight = (1.0 / pair.getSecond().getFirst()) / totalAmbiguity;
            transform = transform.plus(pair.getFirst().getTranslation().times(weight));
            rotation = rotation.plus(pair.getFirst().getRotation().times(weight));
            timestampSum += pair.getSecond().getSecond();
        }

        return Optional.of(Pair.of(new Pose3d(transform, rotation), timestampSum / targetData.size()));
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
}

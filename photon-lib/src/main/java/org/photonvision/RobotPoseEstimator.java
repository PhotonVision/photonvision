package org.photonvision;

import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.photonvision.targeting.PhotonTrackedTarget;

public class RobotPoseEstimator {

    /***
     * <ul>
     *   <li><strong>LOWEST_AMBIGUITY</strong>: Choose the Pose with the lowest ambiguity
     *   <li><strong>CLOSEST_TO_CAMERA_HEIGHT</strong>: Choose the Pose which is closest to the camera
     *       height
     *   <li><strong>CLOSEST_TO_REFERENCE_POSE</strong>: Choose the Pose which is closest to the pose
     *       from setReferencePose()
     *   <li><strong>CLOSEST_TO_LAST_POSE</strong>: Choose the Pose which is closest to the last pose
     *       calculated
     * </ul>
     */
    enum PoseStrategy {
        LOWEST_AMBIGUITY,
        CLOSEST_TO_CAMERA_HEIGHT,
        CLOSEST_TO_REFERENCE_POSE,
        CLOSEST_TO_LAST_POSE,
        AVERAGE_BEST_TARGETS
    }

    private Map<Integer, Pose3d> aprilTags;
    private PoseStrategy strategy;
    private ArrayList<Pair<PhotonCamera, Transform3d>> cameras;
    private Pose3d lastPose;

    private Pose3d referencePose;

    /**
     * Create a new RobotPoseEstimator.
     *
     * <p>Example: <code>
     * <p>
     * Map<Integer, Pose3d> map = new HashMap<>();
     * <p>
     * map.put(1, new Pose3d(1.0, 2.0, 3.0, new Rotation3d())); // Tag ID 1 is at (1.0,2.0,3.0)
     * </code>
     *
     * @param aprilTags A Map linking AprilTag IDs to Pose3ds with respect to the FIRST field.
     * @param strategy The strategy it should use to determine the best pose.
     * @param cameras An ArrayList of Pairs of PhotonCameras and their respective Transform3ds from
     *     the center of the robot to the cameras.
     */
    public RobotPoseEstimator(
            Map<Integer, Pose3d> aprilTags,
            PoseStrategy strategy,
            ArrayList<Pair<PhotonCamera, Transform3d>> cameras) {
        this.aprilTags = aprilTags;
        this.strategy = strategy;
        this.cameras = cameras;
        lastPose = new Pose3d();
    }

    /**
     * Update the estimated pose using the selected strategy.
     *
     * @return The updated estimated pose and the latency in milliseconds
     */
    public Pair<Pose3d, Double> update() {
        if (cameras.isEmpty()) {
            DriverStation.reportError("[RobotPoseEstimator] Missing any camera!", false);
            return Pair.of(lastPose, 0.);
        }
        Pair<Pose3d, Double> pair;
        switch (strategy) {
            case LOWEST_AMBIGUITY:
                pair = lowestAmbiguityStrategy();
                lastPose = pair.getFirst();
                return pair;
            case CLOSEST_TO_CAMERA_HEIGHT:
                pair = closestToCameraHeightStrategy();
                lastPose = pair.getFirst();
                return pair;
            case CLOSEST_TO_REFERENCE_POSE:
                pair = closestToReferencePoseStrategy();
                lastPose = pair.getFirst();
                return pair;
            case CLOSEST_TO_LAST_POSE:
                referencePose = lastPose;
                pair = closestToReferencePoseStrategy();
                lastPose = pair.getFirst();
                return pair;
            case AVERAGE_BEST_TARGETS:
                pair = averageBestTargetsStrategy();
                lastPose = pair.getFirst();
                return pair;
            default:
                DriverStation.reportError("[RobotPoseEstimator] Invalid pose strategy!", false);
                return Pair.of(lastPose, 0.);
        }
    }

    private Pair<Pose3d, Double> lowestAmbiguityStrategy() {
        // Loop over each ambiguity of all the cameras
        int lowestAI = -1;
        int lowestAJ = -1;
        double lowestAmbiguityScore = 10;
        for (int i = 0; i < cameras.size(); i++) {
            Pair<PhotonCamera, Transform3d> p = cameras.get(i);
            List<PhotonTrackedTarget> targets = p.getFirst().getLatestResult().targets;
            for (int j = 0; j < targets.size(); j++) {
                if (targets.get(j).getPoseAmbiguity() < lowestAmbiguityScore) {
                    lowestAI = i;
                    lowestAJ = j;
                    lowestAmbiguityScore = targets.get(j).getPoseAmbiguity();
                }
            }
        }

        // No targets, return the last pose
        if (lowestAI == -1 || lowestAJ == -1) {
            return Pair.of(lastPose, 0.);
        }

        // Pick the lowest and do the heavy calculations
        PhotonTrackedTarget bestTarget =
                cameras.get(lowestAI).getFirst().getLatestResult().targets.get(lowestAJ);

        // If the map doesn't contain the ID fail
        if (!aprilTags.containsKey(bestTarget.getFiducialId())) {
            DriverStation.reportError(
                    "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                            + bestTarget.getFiducialId(),
                    false);
            return Pair.of(lastPose, 0.);
        }

        return Pair.of(
                aprilTags
                        .get(bestTarget.getFiducialId())
                        .transformBy(bestTarget.getBestCameraToTarget().inverse())
                        .transformBy(cameras.get(lowestAI).getSecond().inverse()),
                cameras.get(lowestAI).getFirst().getLatestResult().getLatencyMillis());
    }

    private Pair<Pose3d, Double> closestToCameraHeightStrategy() {
        // Loop over each ambiguity of all the cameras
        double smallestHeightDifference = 10e9;
        double mili = 0;
        Pose3d pose = lastPose;

        for (int i = 0; i < cameras.size(); i++) {
            Pair<PhotonCamera, Transform3d> p = cameras.get(i);
            List<PhotonTrackedTarget> targets = p.getFirst().getLatestResult().targets;
            for (int j = 0; j < targets.size(); j++) {
                PhotonTrackedTarget target = targets.get(j);
                // If the map doesn't contain the ID fail
                if (!aprilTags.containsKey(target.getFiducialId())) {
                    DriverStation.reportWarning(
                            "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                    + target.getFiducialId(),
                            false);
                    continue;
                }
                Pose3d targetPose = aprilTags.get(target.getFiducialId());
                double alternativeDifference =
                        Math.abs(
                                p.getSecond().getZ()
                                        - targetPose.transformBy(target.getAlternateCameraToTarget().inverse()).getZ());
                double bestDifference =
                        Math.abs(
                                p.getSecond().getZ()
                                        - targetPose.transformBy(target.getBestCameraToTarget().inverse()).getZ());
                if (alternativeDifference < smallestHeightDifference) {
                    smallestHeightDifference = alternativeDifference;
                    pose = targetPose.transformBy(target.getAlternateCameraToTarget().inverse());
                    mili = p.getFirst().getLatestResult().getLatencyMillis();
                }
                if (bestDifference < smallestHeightDifference) {
                    smallestHeightDifference = bestDifference;
                    pose = targetPose.transformBy(target.getBestCameraToTarget().inverse());
                    mili = p.getFirst().getLatestResult().getLatencyMillis();
                }
            }
        }
        return Pair.of(pose, mili);
    }

    private Pair<Pose3d, Double> closestToReferencePoseStrategy() {
        if (referencePose == null) {
            DriverStation.reportError(
                    "[RobotPoseEstimator] Tried to use reference pose strategy without setting the reference!",
                    false);
            return Pair.of(lastPose, 0.);
        }
        double smallestDifference = 10e9;
        double mili = 0;
        Pose3d pose = lastPose;
        for (int i = 0; i < cameras.size(); i++) {
            Pair<PhotonCamera, Transform3d> p = cameras.get(i);
            List<PhotonTrackedTarget> targets = p.getFirst().getLatestResult().targets;
            for (int j = 0; j < targets.size(); j++) {
                PhotonTrackedTarget target = targets.get(j);
                // If the map doesn't contain the ID fail
                if (!aprilTags.containsKey(target.getFiducialId())) {
                    DriverStation.reportWarning(
                            "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                    + target.getFiducialId(),
                            false);
                    continue;
                }
                Pose3d targetPose = aprilTags.get(target.getFiducialId());
                double alternativeDifference =
                        Math.abs(
                                calculateDifference(
                                        referencePose,
                                        targetPose.transformBy(target.getAlternateCameraToTarget().inverse())));
                double bestDifference =
                        Math.abs(
                                calculateDifference(
                                        referencePose,
                                        targetPose.transformBy(target.getBestCameraToTarget().inverse())));
                if (alternativeDifference < smallestDifference) {
                    smallestDifference = alternativeDifference;
                    pose = targetPose.transformBy(target.getAlternateCameraToTarget().inverse());
                    mili = p.getFirst().getLatestResult().getLatencyMillis();
                }
                if (bestDifference < smallestDifference) {
                    smallestDifference = bestDifference;
                    pose = targetPose.transformBy(target.getBestCameraToTarget().inverse());
                    mili = p.getFirst().getLatestResult().getLatencyMillis();
                }
            }
        }
        return Pair.of(pose, mili);
    }

    /** Return the average of the best target poses using ambiguity as weight */
    private Pair<Pose3d, Double> averageBestTargetsStrategy() {
        //                  Pair of Double, Double = Ambiguity, Mili
        List<Pair<Pose3d, Pair<Double, Double>>> tempPoses = new ArrayList<>();
        double totalAmbiguity = 0;
        for (int i = 0; i < cameras.size(); i++) {
            Pair<PhotonCamera, Transform3d> p = cameras.get(i);
            List<PhotonTrackedTarget> targets = p.getFirst().getLatestResult().targets;
            for (int j = 0; j < targets.size(); j++) {
                PhotonTrackedTarget target = targets.get(j);
                // If the map doesn't contain the ID fail
                if (!aprilTags.containsKey(target.getFiducialId())) {
                    DriverStation.reportWarning(
                            "[RobotPoseEstimator] Tried to get pose of unknown April Tag: "
                                    + target.getFiducialId(),
                            false);
                    continue;
                }
                Pose3d targetPose = aprilTags.get(target.getFiducialId());
                try {
                    totalAmbiguity += 1. / target.getPoseAmbiguity();
                } catch (ArithmeticException e) {
                    DriverStation.reportWarning(
                            "[RobotPoseEstimator] A total ambiguity of zero exists, using that pose instead!",
                            false);
                    return Pair.of(
                            targetPose.transformBy(target.getBestCameraToTarget().inverse()),
                            p.getFirst().getLatestResult().getLatencyMillis());
                }
                tempPoses.add(
                        Pair.of(
                                targetPose.transformBy(target.getBestCameraToTarget().inverse()),
                                Pair.of(
                                        target.getPoseAmbiguity(), p.getFirst().getLatestResult().getLatencyMillis())));
            }
        }

        Translation3d transform = new Translation3d();
        Rotation3d rotation = new Rotation3d();
        double latency = 0;

        for (Pair<Pose3d, Pair<Double, Double>> pair : tempPoses) {
            try {
                double weight = (1. / pair.getSecond().getFirst()) / totalAmbiguity;
                transform = transform.plus(pair.getFirst().getTranslation().times(weight));
                rotation = rotation.plus(pair.getFirst().getRotation().times(weight));
                latency += pair.getSecond().getSecond() * weight; // NOTE: Average latency may not work well
            } catch (ArithmeticException e) {
                DriverStation.reportWarning(
                        "[RobotPoseEstimator] A total ambiguity of zero exists, using that pose instead!",
                        false);
                return Pair.of(pair.getFirst(), pair.getSecond().getSecond());
            }
        }
        return Pair.of(new Pose3d(transform, rotation), latency);
    }

    /**
     * Difference is defined as the vector magnitude between the two poses
     *
     * @return The absolute "difference" (>=0) between two Pose3ds.
     */
    private double calculateDifference(Pose3d x, Pose3d y) {
        return x.getTranslation().getDistance(y.getTranslation());
    }

    /** @param aprilTags the aprilTags to set */
    public void setAprilTags(Map<Integer, Pose3d> aprilTags) {
        this.aprilTags = aprilTags;
    }

    /** @return the aprilTags */
    public Map<Integer, Pose3d> getAprilTags() {
        return aprilTags;
    }

    /** @return the strategy */
    public PoseStrategy getStrategy() {
        return strategy;
    }

    /** @param strategy the strategy to set */
    public void setStrategy(PoseStrategy strategy) {
        this.strategy = strategy;
    }

    /** @return the referencePose */
    public Pose3d getReferencePose() {
        return referencePose;
    }

    /**
     * Update the stored reference pose for use with CLOSEST_TO_REFERENCE_POSE
     *
     * @param referencePose the referencePose to set
     */
    public void setReferencePose(Pose3d referencePose) {
        this.referencePose = referencePose;
    }

    /**
     * UPdate the stored last pose. Useful for setting the initial estimate with CLOSEST_TO_LAST_POSE
     *
     * @param lastPose the lastPose to set
     */
    public void setLastPose(Pose3d lastPose) {
        this.lastPose = lastPose;
    }
}

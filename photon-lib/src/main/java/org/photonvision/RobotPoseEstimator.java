package org.photonvision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.DriverStation;

public class RobotPoseEstimator {

    /**
     * <ul>
     * <li><strong>LOWEST_AMBIGUITY</strong>: Choose the Pose with the lowest ambiguity</li>
     * <li><strong>CLOSEST_TO_CAMERA_HEIGHT</strong>: Choose the Pose which is closest to the camera height</li>
     * <li><strong>CLOSEST_TO_REFERENCE_POSE</strong>: Choose the Pose which is closest to the pose from setReferencePose()</li>
     * </ul>
     */
    enum PoseStrategy {
        LOWEST_AMBIGUITY, // TODO: Test
        CLOSEST_TO_CAMERA_HEIGHT, // TODO: Implement
        CLOSEST_TO_REFERENCE_POSE // TODO: Implement
    
    }
    
    
    private Map<Integer, Pose3d> aprilTags;
    private PoseStrategy strategy;
    private ArrayList<Pair<PhotonCamera, Transform3d>> cameras;
    private Pose3d lastPose;
    
    /**
     * Create a new RobotPoseEstimator. 
     * <p>
     * Example: 
     *  <code>
     *  <p>
     *  Map<Integer, Pose3d> map = new HashMap<>();
     *  <p>
     *  map.put(1, new Pose3d(1.0, 2.0, 3.0, new Rotation3d())); // Tag ID 1 is at (1.0,2.0,3.0)
     *  </code>
     * 
     * @param aprilTags A Map linking AprilTag IDs to Pose3ds with respect to the FIRST field.
     * @param strategy The strategy it should use to determine the best pose.
     * @param cameras An ArrayList of Pairs of PhotonCameras and their respective Transform3ds from the center of the robot to the cameras. 
     */
    public RobotPoseEstimator(Map<Integer, Pose3d> aprilTags, PoseStrategy strategy, ArrayList<Pair<PhotonCamera, Transform3d>> cameras) {
        this.aprilTags = aprilTags;
        this.strategy = strategy;
        this.cameras = cameras;
    }


    public Pose3d update() {
        if (cameras.isEmpty()) {
            DriverStation.reportError("[RobotPoseEstimator] Missing any camera!", false);
            return lastPose;
        }
        switch (strategy) {
            case LOWEST_AMBIGUITY:
                lastPose = lowestAmbiguityStrategy();
                return lastPose;
            case CLOSEST_TO_CAMERA_HEIGHT:

                return lastPose;
            default:
                DriverStation.reportError("[RobotPoseEstimator] Invalid pose strategy!", false);
                return lastPose;
        }
    }

    private Pose3d lowestAmbiguityStrategy() {
        // Loop over each ambiguity of all the cameras
        int lowestAI = -1;
        int lowestAJ = -1;
        double lowestAmbiguityScore = 10;
        for (int i = 0; i < cameras.size(); i++) {
            Pair<PhotonCamera, Transform3d> p = cameras.get(i);
            List<PhotonTrackedTarget> targets = p.getFirst().getLatestResult().targets;
            for (int j = 0; j < targets.size(); j++) {
                if (targets.get(j).getPoseAmbiguity() > lowestAmbiguityScore) {
                    lowestAI = i;
                    lowestAJ = j;
                    lowestAmbiguityScore = targets.get(j).getPoseAmbiguity();
                }
            }
        }

        // No targets, return the last pose
        if (lowestAI == -1 || lowestAJ == -1) {
            return lastPose;
        }

        // Pick the lowest and do the heavy calculations
        PhotonTrackedTarget bestTarget = cameras.get(lowestAI).getFirst().getLatestResult().targets.get(lowestAJ);

        // If the map doesn't contain the ID fail
        if (!aprilTags.containsKey(bestTarget.getFiducialId())) {
            DriverStation.reportError(
                    "[RobotPoseEstimator] Tried to get pose of unknown April Tag: " + bestTarget.getFiducialId(),
                    false);
            return lastPose;
        }

        return aprilTags.get(bestTarget.getFiducialId())
                .transformBy(bestTarget.getBestCameraToTarget().inverse())
                .transformBy(cameras.get(lowestAI).getSecond().inverse());
    }


    /**
     * Difference is defined as the vector magnitude between the two poses
     * @return The absolute "difference" (>=0) between two Pose3ds. 
     */
    private double calculateDifference(Pose3d x, Pose3d y) {
        return x.getTranslation().getDistance(y.getTranslation());
    }

    /**
     * @param aprilTags the aprilTags to set
     */
    public void setAprilTags(Map<Integer, Pose3d> aprilTags) {
        this.aprilTags = aprilTags;
    }

    /**
     * @return the aprilTags
     */
    public Map<Integer, Pose3d> getAprilTags() {
        return aprilTags;
    }

    
}

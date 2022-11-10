package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.photonvision.RobotPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

class RobotPoseEstimatorTest {
    @Test
    void testLowestAmbiguityStrategy() {
            Map<Integer, Pose3d> aprilTags = new HashMap<>();
            aprilTags.put(0, new Pose3d(3, 3, 3, new Rotation3d()));
            aprilTags.put(1, new Pose3d(5, 5, 5, new Rotation3d()));

            ArrayList<Pair<PhotonCamera, Transform3d>> cameras = new ArrayList<>();

            PhotonCameraInjector cameraOne = new PhotonCameraInjector();
            cameraOne.result = new PhotonPipelineResult(
                            2,
                            List.of(
                                            new PhotonTrackedTarget(
                                                            3.0,
                                                            -4.0,
                                                            9.0,
                                                            4.0,
                                                            0,
                                                            new Transform3d(new Translation3d(1, 2, 3),
                                                                            new Rotation3d(1, 2, 3)),
                                                            new Transform3d(new Translation3d(1, 2, 3),
                                                                            new Rotation3d(1, 2, 3)),
                                                            0.7,
                                                            List.of(
                                                                            new TargetCorner(1, 2),
                                                                            new TargetCorner(3, 4),
                                                                            new TargetCorner(5, 6),
                                                                            new TargetCorner(7, 8))),
                                            new PhotonTrackedTarget(
                                                            3.0,
                                                            -4.0,
                                                            9.1,
                                                            6.7,
                                                            1,
                                                            new Transform3d(new Translation3d(4, 2, 3),
                                                                            new Rotation3d(0, 0, 0)),
                                                            new Transform3d(new Translation3d(4, 2, 3),
                                                                            new Rotation3d(1, 5, 3)),
                                                            0.3,
                                                            List.of(
                                                                            new TargetCorner(1, 2),
                                                                            new TargetCorner(3, 4),
                                                                            new TargetCorner(5, 6),
                                                                            new TargetCorner(7, 8)))));
            PhotonCameraInjector cameraTwo = new PhotonCameraInjector();
            cameraTwo.result = new PhotonPipelineResult(
                            4,
                            List.of(
                                            new PhotonTrackedTarget(
                                                            9.0,
                                                            -2.0,
                                                            19.0,
                                                            3.0,
                                                            0,
                                                            new Transform3d(new Translation3d(1, 2, 3),
                                                                            new Rotation3d(1, 2, 3)),
                                                            new Transform3d(new Translation3d(1, 2, 3),
                                                                            new Rotation3d(1, 2, 3)),
                                                            0.4,
                                                            List.of(
                                                                            new TargetCorner(1, 2),
                                                                            new TargetCorner(3, 4),
                                                                            new TargetCorner(5, 6),
                                                                            new TargetCorner(7, 8)))));

            cameras.add(Pair.of(cameraOne, new Transform3d()));
            cameras.add(Pair.of(cameraTwo, new Transform3d()));

            RobotPoseEstimator estimator = new RobotPoseEstimator(aprilTags, PoseStrategy.LOWEST_AMBIGUITY, cameras);

            Pair<Pose3d, Double> estimatedPose = estimator.update();
            Pose3d pose = estimatedPose.getFirst();

            assertEquals(2, estimatedPose.getSecond());
            assertEquals(1, pose.getX());
            assertEquals(3, pose.getY());
            assertEquals(2, pose.getZ());
    }

    @Test
    void testClosestToCameraHeightStrategy() {
        Map<Integer, Pose3d> aprilTags = new HashMap<>();
        aprilTags.put(0, new Pose3d(3, 3, 3, new Rotation3d()));
        aprilTags.put(1, new Pose3d(5, 5, 5, new Rotation3d()));

        ArrayList<Pair<PhotonCamera, Transform3d>> cameras = new ArrayList<>();

        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        2,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.1,
                                        6.7,
                                        1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));
        PhotonCameraInjector cameraTwo = new PhotonCameraInjector();
        cameraTwo.result =
                new PhotonPipelineResult(
                        4,
                        List.of(
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        new Transform3d(new Translation3d(4, 4, 4), new Rotation3d()),
                                        new Transform3d(new Translation3d(5, 5, 5), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0,0,4), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0,0,2), new Rotation3d())));

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT, cameras);

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();
        
        assertEquals(2, estimatedPose.getSecond());
        assertEquals(4, pose.getX());
        assertEquals(4, pose.getY());
        assertEquals(4, pose.getZ());
    }

    private class PhotonCameraInjector extends PhotonCamera {
        public PhotonCameraInjector() {
            super("Test");
        }

        PhotonPipelineResult result;

        @Override
        public PhotonPipelineResult getLatestResult() {
            return result;
        }
    }
}

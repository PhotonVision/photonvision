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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.RobotPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

class RobotPoseEstimatorTest {
    @BeforeAll
    public static void init() {
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);

        try {
            CombinedRuntimeLoader.loadLibraries(
                    RobotPoseEstimatorTest.class, "wpiutiljni", "ntcorejni", "wpinetjni", "wpiHaljni");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    void testLowestAmbiguityStrategy() {
        List<AprilTag> aprilTags = List.of(
                new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())),
                new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        AprilTagFieldLayout tagLayout = new AprilTagFieldLayout(aprilTags, 10, 10);

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
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
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
                                new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(0, 0, 0)),
                                new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
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
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                0.4,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8)))));

        cameras.add(Pair.of(cameraOne, new Transform3d()));
        cameras.add(Pair.of(cameraTwo, new Transform3d()));

        RobotPoseEstimator estimator = new RobotPoseEstimator(tagLayout, PoseStrategy.LOWEST_AMBIGUITY, cameras);

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();

        assertEquals(2, estimatedPose.getSecond());
        assertEquals(1, pose.getX(), .01);
        assertEquals(3, pose.getY(), .01);
        assertEquals(2, pose.getZ(), .01);
    }

    @Test
    void testClosestToCameraHeightStrategy() {
        List<AprilTag> aprilTags = List.of(
                new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())),
                new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        AprilTagFieldLayout tagLayout = new AprilTagFieldLayout(aprilTags, 10, 10);

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
        cameraTwo.result = new PhotonPipelineResult(
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

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0, 0, 4), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0, 0, 2), new Rotation3d())));

        RobotPoseEstimator estimator = new RobotPoseEstimator(tagLayout, PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT,
                cameras);

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();

        assertEquals(2, estimatedPose.getSecond());
        assertEquals(4, pose.getX(), .01);
        assertEquals(4, pose.getY(), .01);
        assertEquals(4, pose.getZ(), .01);
    }

    @Test
    void closestToReferencePoseStrategy() {
        List<AprilTag> aprilTags = List.of(
                new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())),
                new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        AprilTagFieldLayout tagLayout = new AprilTagFieldLayout(aprilTags, 10, 10);

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
        cameraTwo.result = new PhotonPipelineResult(
                4,
                List.of(
                        new PhotonTrackedTarget(
                                9.0,
                                -2.0,
                                19.0,
                                3.0,
                                0,
                                new Transform3d(new Translation3d(2.2, 2.2, 2.2), new Rotation3d()),
                                new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                0.4,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8)))));

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));

        RobotPoseEstimator estimator = new RobotPoseEstimator(tagLayout, PoseStrategy.CLOSEST_TO_REFERENCE_POSE,
                cameras);
        estimator.setReferencePose(new Pose3d(1, 1, 1, new Rotation3d()));

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();

        assertEquals(4, estimatedPose.getSecond());
        assertEquals(1, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(.9, pose.getZ(), .01);
    }

    @Test
    void closestToLastPose() {
        List<AprilTag> aprilTags = List.of(
                new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())),
                new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        AprilTagFieldLayout tagLayout = new AprilTagFieldLayout(aprilTags, 10, 10);

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
        cameraTwo.result = new PhotonPipelineResult(
                4,
                List.of(
                        new PhotonTrackedTarget(
                                9.0,
                                -2.0,
                                19.0,
                                3.0,
                                0,
                                new Transform3d(new Translation3d(2.2, 2.2, 2.2), new Rotation3d()),
                                new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                0.4,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8)))));

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));

        RobotPoseEstimator estimator = new RobotPoseEstimator(tagLayout, PoseStrategy.CLOSEST_TO_LAST_POSE, cameras);

        estimator.setLastPose(new Pose3d(1, 1, 1, new Rotation3d()));

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();

        cameraOne.result = new PhotonPipelineResult(
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
                                0,
                                new Transform3d(new Translation3d(2.1, 1.9, 2), new Rotation3d()),
                                new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                0.3,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8)))));
        cameraTwo.result = new PhotonPipelineResult(
                4,
                List.of(
                        new PhotonTrackedTarget(
                                9.0,
                                -2.0,
                                19.0,
                                3.0,
                                0,
                                new Transform3d(new Translation3d(2.4, 2.4, 2.2), new Rotation3d()),
                                new Transform3d(new Translation3d(2, 1, 2), new Rotation3d()),
                                0.4,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8)))));

        estimatedPose = estimator.update();
        pose = estimatedPose.getFirst();

        assertEquals(2, estimatedPose.getSecond());
        assertEquals(.9, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(1, pose.getZ(), .01);
    }

    @Test
    void averageBestPoses() {
        List<AprilTag> aprilTags = List.of(
                new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())),
                new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        AprilTagFieldLayout tagLayout = new AprilTagFieldLayout(aprilTags, 10, 10);

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
                                new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                0.7,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8))), // 1 1 1 ambig: .7
                        new PhotonTrackedTarget(
                                3.0,
                                -4.0,
                                9.1,
                                6.7,
                                1,
                                new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                0.3,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8))))); // 2 2 2 ambig .3
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
                                new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                0.4,
                                List.of(
                                        new TargetCorner(1, 2),
                                        new TargetCorner(3, 4),
                                        new TargetCorner(5, 6),
                                        new TargetCorner(7, 8))))); // 3 3 3 ambig .4

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0, 0, 0), new Rotation3d())));

        RobotPoseEstimator estimator = new RobotPoseEstimator(tagLayout, PoseStrategy.AVERAGE_BEST_TARGETS, cameras);

        Pair<Pose3d, Double> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.getFirst();
        assertEquals(2.6885245901639347, estimatedPose.getSecond(), .01);
        assertEquals(2.15, pose.getX(), .01);
        assertEquals(2.15, pose.getY(), .01);
        assertEquals(2.15, pose.getZ(), .01);
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

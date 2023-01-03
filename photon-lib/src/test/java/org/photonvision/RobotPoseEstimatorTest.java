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

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.RobotPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

class RobotPoseEstimatorTest {
    static AprilTagFieldLayout aprilTags;

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

        List<AprilTag> atList = new ArrayList<AprilTag>(2);
        atList.add(new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())));
        atList.add(new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        var fl = Units.feetToMeters(54.0);
        var fw = Units.feetToMeters(27.0);
        aprilTags = new AprilTagFieldLayout(atList, fl, fw);
    }

    @Test
    void testLowestAmbiguityStrategy() {
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

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.LOWEST_AMBIGUITY, cameras);

        Optional<Pair<Pose3d, Double>> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.get().getFirst();

        assertEquals(2, estimatedPose.get().getSecond());
        assertEquals(1, pose.getX(), .01);
        assertEquals(3, pose.getY(), .01);
        assertEquals(2, pose.getZ(), .01);
    }

    @Test
    void testClosestToCameraHeightStrategy() {
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

        cameras.add(Pair.of(cameraOne, new Transform3d(new Translation3d(0, 0, 4), new Rotation3d())));
        cameras.add(Pair.of(cameraTwo, new Transform3d(new Translation3d(0, 0, 2), new Rotation3d())));

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT, cameras);

        Optional<Pair<Pose3d, Double>> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.get().getFirst();

        assertEquals(2, estimatedPose.get().getSecond());
        assertEquals(4, pose.getX(), .01);
        assertEquals(4, pose.getY(), .01);
        assertEquals(0, pose.getZ(), .01);
    }

    @Test
    void closestToReferencePoseStrategy() {
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

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.CLOSEST_TO_REFERENCE_POSE, cameras);
        estimator.setReferencePose(new Pose3d(1, 1, 1, new Rotation3d()));

        Optional<Pair<Pose3d, Double>> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.get().getFirst();

        assertEquals(4, estimatedPose.get().getSecond());
        assertEquals(1, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(.9, pose.getZ(), .01);
    }

    @Test
    void closestToLastPose() {
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

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.CLOSEST_TO_LAST_POSE, cameras);

        estimator.setLastPose(new Pose3d(1, 1, 1, new Rotation3d()));

        Optional<Pair<Pose3d, Double>> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.get().getFirst();

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
                                        0,
                                        new Transform3d(new Translation3d(2.1, 1.9, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));
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
                                        new Transform3d(new Translation3d(2.4, 2.4, 2.2), new Rotation3d()),
                                        new Transform3d(new Translation3d(2, 1, 2), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        estimatedPose = estimator.update();
        pose = estimatedPose.get().getFirst();

        assertEquals(2, estimatedPose.get().getSecond());
        assertEquals(.9, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(1, pose.getZ(), .01);
    }

    @Test
    void averageBestPoses() {
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

        RobotPoseEstimator estimator =
                new RobotPoseEstimator(aprilTags, PoseStrategy.AVERAGE_BEST_TARGETS, cameras);

        Optional<Pair<Pose3d, Double>> estimatedPose = estimator.update();
        Pose3d pose = estimatedPose.get().getFirst();
        assertEquals(2.6885245901639347, estimatedPose.get().getSecond(), .01);
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

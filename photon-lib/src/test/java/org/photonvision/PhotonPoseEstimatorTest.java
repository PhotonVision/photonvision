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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.PhotonPoseEstimator.ConstrainedSolvepnpParams;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import org.photonvision.estimation.TargetModel;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.WpilibLoader;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionTargetSim;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineMetadata;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.PnpResult;
import org.photonvision.targeting.TargetCorner;

class PhotonPoseEstimatorTest {
    static AprilTagFieldLayout aprilTags;

    @BeforeAll
    public static void init() throws UnsatisfiedLinkError, IOException {
        if (!WpilibLoader.loadLibraries()) {
            fail();
        }
        if (!PhotonTargetingJniLoader.load()) {
            fail();
        }

        HAL.initialize(1000, 0);

        List<AprilTag> tagList = new ArrayList<>(2);
        tagList.add(new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())));
        tagList.add(new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        double fieldLength = Units.feetToMeters(54.0);
        double fieldWidth = Units.feetToMeters(27.0);
        aprilTags = new AprilTagFieldLayout(tagList, fieldLength, fieldWidth);
    }

    @AfterAll
    public static void teardown() {
        HAL.shutdown();
    }

    @Test
    void testLowestAmbiguityStrategy() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        11 * 1000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(0, 0, 0)),
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(aprilTags, PoseStrategy.LOWEST_AMBIGUITY, new Transform3d());

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        Pose3d pose = estimatedPose.get().estimatedPose;

        assertEquals(11, estimatedPose.get().timestampSeconds);
        assertEquals(1, pose.getX(), .01);
        assertEquals(3, pose.getY(), .01);
        assertEquals(2, pose.getZ(), .01);
    }

    @Test
    void testClosestToCameraHeightStrategy() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        4000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        1,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(4, 4, 4), new Rotation3d()),
                                        new Transform3d(new Translation3d(5, 5, 5), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.CLOSEST_TO_CAMERA_HEIGHT,
                        new Transform3d(new Translation3d(0, 0, 4), new Rotation3d()));

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        Pose3d pose = estimatedPose.get().estimatedPose;

        assertEquals(4, estimatedPose.get().timestampSeconds);
        assertEquals(4, pose.getX(), .01);
        assertEquals(4, pose.getY(), .01);
        assertEquals(0, pose.getZ(), .01);
    }

    @Test
    void closestToReferencePoseStrategy() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        17000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        1,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2.2, 2.2, 2.2), new Rotation3d()),
                                        new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.CLOSEST_TO_REFERENCE_POSE,
                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()));
        estimator.setReferencePose(new Pose3d(1, 1, 1, new Rotation3d()));

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        Pose3d pose = estimatedPose.get().estimatedPose;

        assertEquals(17, estimatedPose.get().timestampSeconds);
        assertEquals(1, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(.9, pose.getZ(), .01);
    }

    @Test
    void closestToLastPose() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        1000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        1,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2.2, 2.2, 2.2), new Rotation3d()),
                                        new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.CLOSEST_TO_LAST_POSE,
                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()));

        estimator.setLastPose(new Pose3d(1, 1, 1, new Rotation3d()));

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        Pose3d pose = estimatedPose.get().estimatedPose;

        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        7000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        1,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2.1, 1.9, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2.4, 2.4, 2.2), new Rotation3d()),
                                        new Transform3d(new Translation3d(2, 1, 2), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        estimatedPose = estimator.update(cameraOne.result);
        pose = estimatedPose.get().estimatedPose;

        assertEquals(7, estimatedPose.get().timestampSeconds);
        assertEquals(.9, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(1, pose.getZ(), .01);
    }

    @Test
    void pnpDistanceTrigSolve() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        PhotonCameraSim cameraOneSim =
                new PhotonCameraSim(cameraOne, SimCameraProperties.PERFECT_90DEG());

        List<VisionTargetSim> simTargets =
                aprilTags.getTags().stream()
                        .map((AprilTag x) -> new VisionTargetSim(x.pose, TargetModel.kAprilTag36h11, x.ID))
                        .toList();

        /* Compound Rolled + Pitched + Yaw */

        Transform3d compoundTestTransform =
                new Transform3d(
                        -Units.inchesToMeters(12),
                        -Units.inchesToMeters(11),
                        3,
                        new Rotation3d(
                                Units.degreesToRadians(37), Units.degreesToRadians(6), Units.degreesToRadians(60)));

        var estimator =
                new PhotonPoseEstimator(
                        aprilTags, PoseStrategy.PNP_DISTANCE_TRIG_SOLVE, compoundTestTransform);

        /* this is the real pose of the robot base we test against */
        var realPose = new Pose3d(7.3, 4.42, 0, new Rotation3d(0, 0, 2.197));
        PhotonPipelineResult result =
                cameraOneSim.process(
                        1, realPose.transformBy(estimator.getRobotToCameraTransform()), simTargets);

        estimator.addHeadingData(result.getTimestampSeconds(), realPose.getRotation().toRotation2d());

        var estimatedPose = estimator.update(result);
        var pose = estimatedPose.get().estimatedPose;

        assertEquals(realPose.getX(), pose.getX(), .01);
        assertEquals(realPose.getY(), pose.getY(), .01);
        assertEquals(0.0, pose.getZ(), .01);

        /* Straight on */

        Transform3d straightOnTestTransform = new Transform3d(0, 0, 3, new Rotation3d(0, 0, 0));

        estimator.setRobotToCameraTransform(straightOnTestTransform);

        /* Pose to compare with */
        realPose = new Pose3d(4.81, 2.38, 0, new Rotation3d(0, 0, 2.818));
        result =
                cameraOneSim.process(
                        1, realPose.transformBy(estimator.getRobotToCameraTransform()), simTargets);

        estimator.addHeadingData(result.getTimestampSeconds(), realPose.getRotation().toRotation2d());

        estimatedPose = estimator.update(result);
        pose = estimatedPose.get().estimatedPose;

        assertEquals(realPose.getX(), pose.getX(), .01);
        assertEquals(realPose.getY(), pose.getY(), .01);
        assertEquals(0.0, pose.getZ(), .01);
    }

    @Test
    void cacheIsInvalidated() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        var result =
                new PhotonPipelineResult(
                        0,
                        20000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.AVERAGE_BEST_TARGETS,
                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()));

        // Empty result, expect empty result
        cameraOne.result = new PhotonPipelineResult();
        cameraOne.result.metadata.captureTimestampMicros = (long) (1 * 1e6);
        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        assertFalse(estimatedPose.isPresent());

        // Set actual result
        cameraOne.result = result;
        estimatedPose = estimator.update(cameraOne.result);
        assertTrue(estimatedPose.isPresent());
        assertEquals(20, estimatedPose.get().timestampSeconds, .01);
        assertEquals(20, estimator.poseCacheTimestampSeconds);

        // And again -- pose cache should mean this is empty
        cameraOne.result = result;
        estimatedPose = estimator.update(cameraOne.result);
        assertFalse(estimatedPose.isPresent());
        // Expect the old timestamp to still be here
        assertEquals(20, estimator.poseCacheTimestampSeconds);

        // Set new field layout -- right after, the pose cache timestamp should be -1
        estimator.setFieldTags(new AprilTagFieldLayout(List.of(new AprilTag(0, new Pose3d())), 0, 0));
        assertEquals(-1, estimator.poseCacheTimestampSeconds);
        // Update should cache the current timestamp (20) again
        cameraOne.result = result;
        estimatedPose = estimator.update(cameraOne.result);
        assertEquals(20, estimatedPose.get().timestampSeconds, .01);
        assertEquals(20, estimator.poseCacheTimestampSeconds);
    }

    @Test
    void averageBestPoses() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        20 * 1000000,
                        1100000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(2, 2, 2), new Rotation3d()),
                                        new Transform3d(new Translation3d(1, 1, 1), new Rotation3d()),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        new Transform3d(new Translation3d(3, 3, 3), new Rotation3d()),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))), // 2 2 2 ambig .3
                                new PhotonTrackedTarget(
                                        9.0,
                                        -2.0,
                                        19.0,
                                        3.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()),
                                        new Transform3d(new Translation3d(2, 1.9, 2.1), new Rotation3d()),
                                        0.4,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))))); // 3 3 3 ambig .4

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.AVERAGE_BEST_TARGETS,
                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()));

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(cameraOne.result);
        Pose3d pose = estimatedPose.get().estimatedPose;

        assertEquals(20, estimatedPose.get().timestampSeconds, .01);
        assertEquals(2.15, pose.getX(), .01);
        assertEquals(2.15, pose.getY(), .01);
        assertEquals(2.15, pose.getZ(), .01);
    }

    @Test
    void testMultiTagOnRioFallback() {
        PhotonCameraInjector camera = new PhotonCameraInjector();
        camera.result =
                new PhotonPipelineResult(
                        0,
                        11 * 1_000_000,
                        1_100_000,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        0,
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        0.7,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
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
                                        -1,
                                        -1,
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(0, 0, 0)),
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        0.3,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));
        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(aprilTags, PoseStrategy.MULTI_TAG_PNP_ON_RIO, Transform3d.kZero);
        estimator.setMultiTagFallbackStrategy(PoseStrategy.LOWEST_AMBIGUITY);

        Optional<EstimatedRobotPose> estimatedPose = estimator.update(camera.result);
        Pose3d pose = estimatedPose.get().estimatedPose;
        // Make sure values match what we'd expect for the LOWEST_AMBIGUITY strategy
        assertAll(
                () -> assertEquals(11, estimatedPose.get().timestampSeconds),
                () -> assertEquals(1, pose.getX(), 1e-9),
                () -> assertEquals(3, pose.getY(), 1e-9),
                () -> assertEquals(2, pose.getZ(), 1e-9));
    }

    @Test
    public void testConstrainedPnpOneTag() {
        var distortion = VecBuilder.fill(0, 0, 0, 0, 0, 0, 0, 0);
        var cameraMat =
                MatBuilder.fill(
                        Nat.N3(),
                        Nat.N3(),
                        399.37500000000006,
                        0,
                        319.5,
                        0,
                        399.16666666666674,
                        239.5,
                        0,
                        0,
                        1);

        /*
         * Ground truth:
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/rotation/q/w,0.
         * 31064262190452635
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/rotation/q/x,0.
         * 24478552235412665
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/rotation/q/y,-0.
         * 0836470779150917
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/rotation/q/z,0.
         * 914649865171567
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/translation/x,3.
         * 191446451763934
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/translation/y,4.
         * 44396966389316
         * 29.989279,NT:/photonvision/YOUR CAMERA
         * NAME/rawBytes/multitagResult/estimatedPose/best/translation/z,0.
         * 4995793771070878
         */
        List<TargetCorner> corners8 =
                List.of(
                        new TargetCorner(98.09875447066685, 331.0093220119495),
                        new TargetCorner(122.20226758624413, 335.50083894738486),
                        new TargetCorner(127.17118732489361, 313.81406314178633),
                        new TargetCorner(104.28543773760417, 309.6516557438994));

        var result =
                new PhotonPipelineResult(
                        new PhotonPipelineMetadata(10000, 2000, 1, 100),
                        List.of(
                                new PhotonTrackedTarget(0, 0, 0, 0, 8, 0, 0, null, null, 0, corners8, corners8)),
                        Optional.of(
                                new MultiTargetPNPResult(
                                        new PnpResult(
                                                new Transform3d(
                                                        // From ground truth
                                                        new Translation3d(
                                                                3.1665557336121353, 4.430673446050584, 0.48678786477534686),
                                                        new Rotation3d(
                                                                new Quaternion(
                                                                        0.3132532247418243,
                                                                        0.24722671090692333,
                                                                        -0.08413452932300695,
                                                                        0.9130568172784148))),
                                                0.1),
                                        new ArrayList<Short>(8))));

        final double camPitch = Units.degreesToRadians(30.0);
        final Transform3d kRobotToCam =
                new Transform3d(new Translation3d(0.5, 0.0, 0.5), new Rotation3d(0, -camPitch, 0));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        AprilTagFieldLayout.loadField(AprilTagFields.k2024Crescendo),
                        PoseStrategy.CONSTRAINED_SOLVEPNP,
                        kRobotToCam);

        estimator.addHeadingData(result.getTimestampSeconds(), Rotation2d.kZero);

        Optional<EstimatedRobotPose> estimatedPose =
                estimator.update(
                        result,
                        Optional.of(cameraMat),
                        Optional.of(distortion),
                        Optional.of(new ConstrainedSolvepnpParams(true, 0)));
        Pose3d pose = estimatedPose.get().estimatedPose;
        System.out.println(pose);
    }

    @Test
    void testConstrainedPnpEmptyCase() {
        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        AprilTagFieldLayout.loadField(AprilTagFields.k2024Crescendo),
                        PoseStrategy.CONSTRAINED_SOLVEPNP,
                        Transform3d.kZero);
        PhotonPipelineResult result = new PhotonPipelineResult();
        var estimate = estimator.update(result);
        assertEquals(estimate, Optional.empty());
    }

    private static class PhotonCameraInjector extends PhotonCamera {
        public PhotonCameraInjector() {
            super("Test");
        }

        PhotonPipelineResult result;

        @Override
        public List<PhotonPipelineResult> getAllUnreadResults() {
            return List.of(result);
        }

        @Override
        public PhotonPipelineResult getLatestResult() {
            return result;
        }
    }
}

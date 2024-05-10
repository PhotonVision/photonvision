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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

class PhotonPoseEstimatorTest {
    static AprilTagFieldLayout aprilTags;

    @BeforeAll
    public static void init() {
        List<AprilTag> tagList = new ArrayList<>(2);
        tagList.add(new AprilTag(0, new Pose3d(3, 3, 3, new Rotation3d())));
        tagList.add(new AprilTag(1, new Pose3d(5, 5, 5, new Rotation3d())));
        double fieldLength = Units.feetToMeters(54.0);
        double fieldWidth = Units.feetToMeters(27.0);
        aprilTags = new AprilTagFieldLayout(tagList, fieldLength, fieldWidth);
    }

    @Test
    void testLowestAmbiguityStrategy() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        cameraOne.result =
                new PhotonPipelineResult(
                        0,
                        0,
                        0,
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
        cameraOne.result.setRecieveTimestampMicros((long) (11 * 1e6));

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
                        0,
                        0,
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

        cameraOne.result.setRecieveTimestampMicros((long) (4 * 1e6));

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
                        0,
                        0,
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
        cameraOne.result.setRecieveTimestampMicros((long) (17 * 1e6));

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
                        0,
                        0,
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
        cameraOne.result.setRecieveTimestampMicros((long) (1 * 1e6));

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
                        0,
                        0,
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
        cameraOne.result.setRecieveTimestampMicros((long) (7 * 1e6));

        estimatedPose = estimator.update(cameraOne.result);
        pose = estimatedPose.get().estimatedPose;

        assertEquals(7, estimatedPose.get().timestampSeconds);
        assertEquals(.9, pose.getX(), .01);
        assertEquals(1.1, pose.getY(), .01);
        assertEquals(1, pose.getZ(), .01);
    }

    @Test
    void cacheIsInvalidated() {
        PhotonCameraInjector cameraOne = new PhotonCameraInjector();
        var result =
                new PhotonPipelineResult(
                        0,
                        0,
                        0,
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
        result.setRecieveTimestampMicros((long) (20 * 1e6));

        PhotonPoseEstimator estimator =
                new PhotonPoseEstimator(
                        aprilTags,
                        PoseStrategy.AVERAGE_BEST_TARGETS,
                        new Transform3d(new Translation3d(0, 0, 0), new Rotation3d()));

        // Empty result, expect empty result
        cameraOne.result = new PhotonPipelineResult();
        cameraOne.result.setRecieveTimestampMicros((long) (1 * 1e6));
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
                        0,
                        0,
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
        cameraOne.result.setRecieveTimestampMicros(20 * 1000000);

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

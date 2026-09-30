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

import org.junit.jupiter.api.Test;
import org.wpilib.math.geometry.*;
import org.wpilib.math.util.Units;

class PhotonUtilTest {
    @Test
    public void testDistance() {
        var camHeight = 1;
        var targetHeight = 3;
        var camPitch = Units.degreesToRadians(0);
        var targetPitch = Units.degreesToRadians(30);

        var dist =
                PhotonUtils.calculateDistanceToTargetMeters(camHeight, targetHeight, camPitch, targetPitch);

        assertEquals(3.464, dist, 0.01);

        camHeight = 1;
        targetHeight = 2;
        camPitch = Units.degreesToRadians(20);
        targetPitch = Units.degreesToRadians(-10);

        dist =
                PhotonUtils.calculateDistanceToTargetMeters(camHeight, targetHeight, camPitch, targetPitch);
        assertEquals(5.671, dist, 0.01);
    }

    @Test
    public void testTransform() {
        var camHeight = 1;
        var tgtHeight = 3;
        var camPitch = 0;
        var tgtPitch = Units.degreesToRadians(30);
        var tgtYaw = new Rotation2d();
        var gyroAngle = new Rotation2d();
        var fieldToTarget = new Pose2d();
        var cameraToRobot = new Transform2d();

        var fieldToRobot =
                PhotonUtils.estimateFieldToRobot(
                        PhotonUtils.estimateCameraToTarget(
                                PhotonUtils.estimateCameraToTargetTranslation(
                                        PhotonUtils.calculateDistanceToTargetMeters(
                                                camHeight, tgtHeight, camPitch, tgtPitch),
                                        tgtYaw),
                                fieldToTarget,
                                gyroAngle),
                        fieldToTarget,
                        cameraToRobot);

        assertEquals(-3.464, fieldToRobot.getX(), 0.1);
        assertEquals(0, fieldToRobot.getY(), 0.1);
        assertEquals(0, fieldToRobot.getRotation().getDegrees(), 0.1);
    }

    @Test
    public void testAprilTagUtils() {
        var cameraToTarget = new Transform3d(new Translation3d(1, 0, 0), new Rotation3d());
        var tagPose = new Pose3d(5, 0, 0, new Rotation3d());
        var cameraToRobot = new Transform3d();

        var fieldToRobot =
                PhotonUtils.estimateFieldToRobotAprilTag(cameraToTarget, tagPose, cameraToRobot);

        var targetPose =
                new Pose2d(
                        new Translation2d(Units.inchesToMeters(324), Units.inchesToMeters(162)),
                        new Rotation2d());
        var currentPose = new Pose2d(0, 0, Rotation2d.fromDegrees(0));
        assertEquals(4.0, fieldToRobot.getX());
        assertEquals(
                Math.toDegrees(Math.atan2((Units.inchesToMeters(162)), (Units.inchesToMeters(324)))),
                PhotonUtils.getYawToPose(currentPose, targetPose).getDegrees());
    }

    @Test
    public void testEstimateCameraToTargetTranslation() {
        // A target 1 meter away and 30 degrees to the left of the camera axis.
        var translation = PhotonUtils.estimateCameraToTargetTranslation(1, Rotation2d.fromDegrees(30));

        assertEquals(0.866, translation.getX(), 0.01);
        assertEquals(0.5, translation.getY(), 0.01);

        // Straight ahead leaves all of the distance on the x axis.
        translation = PhotonUtils.estimateCameraToTargetTranslation(2, new Rotation2d());

        assertEquals(2.0, translation.getX(), 0.01);
        assertEquals(0.0, translation.getY(), 0.01);
    }

    @Test
    public void testEstimateCameraToTarget() {
        // Target 1 meter straight ahead of the camera, robot rotated 30 degrees.
        var cameraToTarget =
                PhotonUtils.estimateCameraToTarget(
                        new Translation2d(1, 0),
                        new Pose2d(7, 3, new Rotation2d()),
                        Rotation2d.fromDegrees(30));

        assertEquals(1.0, cameraToTarget.getX(), 0.01);
        assertEquals(0.0, cameraToTarget.getY(), 0.01);
        // The rotation is -(gyro + fieldToTarget rotation)
        assertEquals(-30.0, cameraToTarget.getRotation().getDegrees(), 0.01);
    }

    @Test
    public void testEstimateFieldToCamera() {
        // The target sits 1 meter directly in front of the camera, and the
        // field-to-target pose puts it at x = 5 facing down +x.
        var cameraToTarget = new Transform2d(new Translation2d(1, 0), new Rotation2d());
        var fieldToTarget = new Pose2d(5, 0, new Rotation2d());

        var fieldToCamera = PhotonUtils.estimateFieldToCamera(cameraToTarget, fieldToTarget);

        assertEquals(4.0, fieldToCamera.getX(), 1e-6);
        assertEquals(0.0, fieldToCamera.getY(), 1e-6);
        assertEquals(0.0, fieldToCamera.getRotation().getDegrees(), 1e-6);

        // Rotating the target 90 degrees moves the camera onto the target's -y axis.
        var rotatedFieldToTarget = new Pose2d(2, 2, Rotation2d.fromDegrees(90));

        fieldToCamera = PhotonUtils.estimateFieldToCamera(cameraToTarget, rotatedFieldToTarget);

        assertEquals(2.0, fieldToCamera.getX(), 1e-6);
        assertEquals(1.0, fieldToCamera.getY(), 1e-6);
        assertEquals(90.0, fieldToCamera.getRotation().getDegrees(), 1e-6);
    }

    @Test
    public void testEstimateFieldToRobot() {
        var cameraToTarget = new Transform2d(new Translation2d(1, 0), new Rotation2d());
        var fieldToTarget = new Pose2d(5, 0, new Rotation2d());
        // The robot sits half a meter in front of the camera.
        var cameraToRobot = new Transform2d(new Translation2d(0.5, 0), new Rotation2d());

        var fieldToRobot =
                PhotonUtils.estimateFieldToRobot(cameraToTarget, fieldToTarget, cameraToRobot);

        // Camera at x = 4, robot half a meter further along +x.
        assertEquals(4.5, fieldToRobot.getX(), 1e-6);
        assertEquals(0.0, fieldToRobot.getY(), 1e-6);
    }

    @Test
    public void testEstimateFieldToRobotFromMeasurements() {
        // The same result as testEstimateFieldToRobot, but going through the
        // overload that takes the heights and pitches instead of a Transform2d.
        var fieldToRobot =
                PhotonUtils.estimateFieldToRobot(
                        1,
                        3,
                        0,
                        Units.degreesToRadians(30),
                        new Rotation2d(),
                        new Rotation2d(),
                        new Pose2d(5, 5, new Rotation2d()),
                        new Transform2d(new Translation2d(0.5, 0), new Rotation2d()));

        // Range is 3.464, so the camera lands at (1.536, 5.0), plus the 0.5
        // camera-to-robot offset.
        assertEquals(2.036, fieldToRobot.getX(), 0.01);
        assertEquals(5.0, fieldToRobot.getY(), 0.01);
    }

    @Test
    public void testGetDistanceToPose() {
        var robotPose = new Pose2d(0, 0, Rotation2d.fromDegrees(0));
        var targetPose = new Pose2d(3, 4, Rotation2d.fromDegrees(0));

        assertEquals(5.0, PhotonUtils.getDistanceToPose(robotPose, targetPose), 1e-9);

        // Only the translations matter, so the headings do not change the result.
        assertEquals(
                5.0,
                PhotonUtils.getDistanceToPose(
                        new Pose2d(0, 0, Rotation2d.fromDegrees(90)),
                        new Pose2d(3, 4, Rotation2d.fromDegrees(-45))),
                1e-9);

        // ...and it does not matter which pose is the robot.
        assertEquals(5.0, PhotonUtils.getDistanceToPose(targetPose, robotPose), 1e-9);

        assertEquals(0.0, PhotonUtils.getDistanceToPose(robotPose, robotPose), 1e-9);
    }
}

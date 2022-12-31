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

import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.util.Units;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PhotonUtilTest {
    @Test
    public void testDistance() {
        var camHeight = 1;
        var targetHeight = 3;
        var camPitch = Units.degreesToRadians(0);
        var targetPitch = Units.degreesToRadians(30);

        var dist =
                PhotonUtils.calculateDistanceToTargetMeters(camHeight, targetHeight, camPitch, targetPitch);

        Assertions.assertEquals(3.464, dist, 0.01);

        camHeight = 1;
        targetHeight = 2;
        camPitch = Units.degreesToRadians(20);
        targetPitch = Units.degreesToRadians(-10);

        dist =
                PhotonUtils.calculateDistanceToTargetMeters(camHeight, targetHeight, camPitch, targetPitch);
        Assertions.assertEquals(5.671, dist, 0.01);
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

        Assertions.assertEquals(-3.464, fieldToRobot.getX(), 0.1);
        Assertions.assertEquals(0, fieldToRobot.getY(), 0.1);
        Assertions.assertEquals(0, fieldToRobot.getRotation().getDegrees(), 0.1);
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
        Assertions.assertEquals(4.0, fieldToRobot.getX());
        Assertions.assertEquals(
                Math.toDegrees(Math.atan2((Units.inchesToMeters(162)), (Units.inchesToMeters(324)))),
                PhotonUtils.getYawToPose(currentPose, targetPose).getDegrees());
    }
}

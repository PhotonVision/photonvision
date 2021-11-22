/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.photonvision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
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
}

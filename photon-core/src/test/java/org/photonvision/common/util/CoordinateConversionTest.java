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

package org.photonvision.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.math.MathUtils;

public class CoordinateConversionTest {
    @BeforeAll
    public static void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void testAprilTagToOpenCV() {
        // AprilTag and OpenCV both use the EDN coordinate system. AprilTag, however, assumes the tag's
        // z-axis points away from the camera while we expect it to point towards the camera.
        var apriltag =
                new Transform3d(
                        new Translation3d(1, 2, 3),
                        new Rotation3d(Math.toRadians(5), Math.toRadians(10), Math.toRadians(15)));
        var opencv = MathUtils.convertApriltagtoOpenCV(apriltag);
        final var expectedTrl = new Translation3d(1, 2, 3);
        assertEquals(
                expectedTrl, opencv.getTranslation(), "AprilTag to OpenCV translation conversion failed");
        var apriltagXaxis = new Translation3d(1, 0, 0).rotateBy(apriltag.getRotation());
        var apriltagYaxis = new Translation3d(0, 1, 0).rotateBy(apriltag.getRotation());
        var apriltagZaxis = new Translation3d(0, 0, 1).rotateBy(apriltag.getRotation());
        var opencvXaxis = new Translation3d(1, 0, 0).rotateBy(opencv.getRotation());
        var opencvYaxis = new Translation3d(0, 1, 0).rotateBy(opencv.getRotation());
        var opencvZaxis = new Translation3d(0, 0, 1).rotateBy(opencv.getRotation());
        assertEquals(
                apriltagXaxis.unaryMinus(),
                opencvXaxis,
                "AprilTag to OpenCV rotation conversion failed(X-axis)");
        assertEquals(
                apriltagYaxis, opencvYaxis, "AprilTag to OpenCV rotation conversion failed(Y-axis)");
        assertEquals(
                apriltagZaxis.unaryMinus(),
                opencvZaxis,
                "AprilTag to OpenCV rotation conversion failed(Z-axis)");
    }

    @Test
    public void testOpenCVToPhoton() {
        // OpenCV uses the EDN coordinate system while wpilib is in NWU.
        var opencv =
                new Transform3d(
                        new Translation3d(1, 2, 3), new Rotation3d(VecBuilder.fill(1, 2, 3), Math.PI / 8));
        var wpilib = MathUtils.convertOpenCVtoPhotonTransform(opencv);
        final var expectedTrl = new Translation3d(3, -1, -2);
        assertEquals(
                expectedTrl, wpilib.getTranslation(), "OpenCV to WPILib translation conversion failed");
        var expectedRot = new Rotation3d(VecBuilder.fill(3, -1, -2), Math.PI / 8);
        assertEquals(expectedRot, wpilib.getRotation(), "OpenCV to WPILib rotation conversion failed");
    }
}

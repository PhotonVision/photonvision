/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.photonvision.common.util.TestUtils;

public class TargetCalculationsTest {

    private static Size imageSize = new Size(800, 600);
    private static Point imageCenterPoint = new Point(imageSize.width / 2, imageSize.height / 2);
    private static double CameraHorizontalFocalLength = 61;
    private static double CameraVerticalFocalLength = 34.3;

    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void yawTest() {
        var targetPixelOffsetX = 100;
        var targetCenterPoint = new Point(imageCenterPoint.x + targetPixelOffsetX, imageCenterPoint.y);

        var yaw =
                TargetCalculations.calculateYaw(
                        imageCenterPoint.x, targetCenterPoint.x, CameraHorizontalFocalLength);

        assertEquals(-1.466, yaw, 0.025, "Yaw not as expected");
    }

    @Test
    public void pitchTest() {
        var targetPixelOffsetY = 100;
        var targetCenterPoint = new Point(imageCenterPoint.x, imageCenterPoint.y + targetPixelOffsetY);

        var pitch =
                TargetCalculations.calculatePitch(
                        imageCenterPoint.y, targetCenterPoint.y, CameraVerticalFocalLength);

        assertEquals(2.607, pitch, 0.025, "Pitch not as expected");
    }

    @Test
    public void targetOffsetTest() {
        Point center = new Point(0, 0);
        Size rectSize = new Size(10, 5);
        double angle = 30;
        RotatedRect rect = new RotatedRect(center, rectSize, angle);
        Point result =
                TargetCalculations.calculateTargetOffsetPoint(false, TargetOffsetPointEdge.Top, rect);
        assertEquals(4.3, result.x, 0.33, "Target offset x not as expected");
        assertEquals(2.5, result.y, 0.05, "Target offset Y not as expected");
    }
}

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
package org.photonvision.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.DualOffsetValues;

public class TargetCalculationsTest {

    private static Size imageSize = new Size(800, 600);
    private static Point imageCenterPoint = new Point(imageSize.width / 2, imageSize.height / 2);
    private static final double diagFOV = Math.toRadians(70.0);

    private static final FrameStaticProperties props =
            new FrameStaticProperties((int) imageSize.width, (int) imageSize.height, diagFOV, null);
    private static final TrackedTarget.TargetCalculationParameters params =
            new TrackedTarget.TargetCalculationParameters(
                    true,
                    TargetOffsetPointEdge.Center,
                    RobotOffsetPointMode.None,
                    new Point(),
                    new DualOffsetValues(),
                    imageCenterPoint,
                    props.horizontalFocalLength,
                    props.verticalFocalLength,
                    imageSize.width * imageSize.height);

    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void yawTest() {
        var targetPixelOffsetX = 100;
        var targetCenterPoint = new Point(imageCenterPoint.x + targetPixelOffsetX, imageCenterPoint.y);

        var trueYaw =
                Math.atan((imageCenterPoint.x - targetCenterPoint.x) / params.horizontalFocalLength);

        var yaw =
                TargetCalculations.calculateYaw(
                        imageCenterPoint.x, targetCenterPoint.x, params.horizontalFocalLength);

        assertEquals(Math.toDegrees(trueYaw), yaw, 0.025, "Yaw not as expected");
    }

    @Test
    public void pitchTest() {
        var targetPixelOffsetY = 100;
        var targetCenterPoint = new Point(imageCenterPoint.x, imageCenterPoint.y + targetPixelOffsetY);

        var truePitch =
                Math.atan((imageCenterPoint.y - targetCenterPoint.y) / params.verticalFocalLength);

        var pitch =
                TargetCalculations.calculatePitch(
                        imageCenterPoint.y, targetCenterPoint.y, params.verticalFocalLength);

        assertEquals(Math.toDegrees(truePitch) * -1, pitch, 0.025, "Pitch not as expected");
    }

    @Test
    public void targetOffsetTest() {
        Point center = new Point(0, 0);
        Size rectSize = new Size(10, 5);
        double angle = 30;
        RotatedRect rect = new RotatedRect(center, rectSize, angle);

        // We pretend like x/y are in pixels, so the "top" is actually the bottom
        var result =
                TargetCalculations.calculateTargetOffsetPoint(true, TargetOffsetPointEdge.Top, rect);
        assertEquals(1.25, result.x, 0.1, "Target offset x not as expected");
        assertEquals(-2.17, result.y, 0.1, "Target offset Y not as expected");
        result =
                TargetCalculations.calculateTargetOffsetPoint(true, TargetOffsetPointEdge.Bottom, rect);
        assertEquals(-1.25, result.x, 0.1, "Target offset x not as expected");
        assertEquals(2.17, result.y, 0.1, "Target offset Y not as expected");
    }

    public static void main(String[] args) {
        TestUtils.loadLibraries();
        new TargetCalculationsTest().targetOffsetTest();
    }

    @Test
    public void testSkewCalculation() {
        // Setup
        var isLandscape = true;
        var rect = new RotatedRect(new Point(), new Size(10, 5), 170);

        // Compute min area rect
        var points = new Point[4];
        rect.points(points);
        var mat2f = new MatOfPoint2f(points);
        var minAreaRect = Imgproc.minAreaRect(mat2f);

        // Assert result
        var result = TargetCalculations.calculateSkew(isLandscape, minAreaRect);
        assertEquals(-10, result, 0.01);

        // Setup
        isLandscape = true;
        rect = new RotatedRect(new Point(), new Size(10, 5), -70);

        // Compute min area rect
        points = new Point[4];
        rect.points(points);
        mat2f.release();
        mat2f = new MatOfPoint2f(points);
        minAreaRect = Imgproc.minAreaRect(mat2f);

        // Assert result
        result = TargetCalculations.calculateSkew(isLandscape, minAreaRect);
        assertEquals(-70, result, 0.01);

        // Setup
        isLandscape = false;
        rect = new RotatedRect(new Point(), new Size(5, 10), 10);

        // Compute min area rect
        points = new Point[4];
        rect.points(points);
        mat2f.release();
        mat2f = new MatOfPoint2f(points);
        minAreaRect = Imgproc.minAreaRect(mat2f);

        // Assert result
        result = TargetCalculations.calculateSkew(isLandscape, minAreaRect);
        assertEquals(10, result, 0.01);

        // Setup
        isLandscape = false;
        rect = new RotatedRect(new Point(), new Size(5, 10), 70);

        // Compute min area rect
        points = new Point[4];
        rect.points(points);
        mat2f.release();
        mat2f = new MatOfPoint2f(points);
        minAreaRect = Imgproc.minAreaRect(mat2f);

        // Assert result
        result = TargetCalculations.calculateSkew(isLandscape, minAreaRect);
        assertEquals(70, result, 0.01);

        // Setup
        isLandscape = false;
        rect = new RotatedRect(new Point(), new Size(5, 10), -70);

        // Compute min area rect
        points = new Point[4];
        rect.points(points);
        mat2f.release();
        mat2f = new MatOfPoint2f(points);
        minAreaRect = Imgproc.minAreaRect(mat2f);

        // Assert result
        result = TargetCalculations.calculateSkew(isLandscape, minAreaRect);
        assertEquals(-70, result, 0.01);
    }

    @Test
    public void testCameraFOVCalculation() {
        final DoubleCouple glowormHorizVert =
                FrameStaticProperties.calculateHorizontalVerticalFoV(74.8, 640, 480);
        var gwHorizDeg = Math.toDegrees(glowormHorizVert.getFirst());
        var gwVertDeg = Math.toDegrees(glowormHorizVert.getSecond());
        assertEquals(62.7, gwHorizDeg, .3);
        assertEquals(49, gwVertDeg, .3);
    }

    @Test
    public void robotOffsetDualTest() {
        final DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        new Point(400, 150), 10,
                        new Point(390, 260), 2);

        final Point expectedHalfway = new Point(393.75, 218.75);
        final Point expectedOutside = new Point(388.75, 273.75);

        Point crosshairPointHalfway =
                TargetCalculations.calculateDualOffsetCrosshair(dualOffsetValues, 5);
        Point crosshairPointOutside =
                TargetCalculations.calculateDualOffsetCrosshair(dualOffsetValues, 1);

        Assertions.assertEquals(expectedHalfway.x, crosshairPointHalfway.x);
        Assertions.assertEquals(expectedHalfway.y, crosshairPointHalfway.y);
        Assertions.assertEquals(expectedOutside.x, crosshairPointOutside.x);
        Assertions.assertEquals(expectedOutside.y, crosshairPointOutside.y);
    }
}

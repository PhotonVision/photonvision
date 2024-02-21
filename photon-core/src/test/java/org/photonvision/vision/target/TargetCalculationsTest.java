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
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.DualOffsetValues;

public class TargetCalculationsTest {

    private static Size imageSize = new Size(800, 600);
    private static Point imageCenterPoint =
            new Point(imageSize.width / 2.0 - 0.5, imageSize.height / 2.0 - 0.5);
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

    @BeforeAll
    public static void setup() {
        TestUtils.loadLibraries();
    }

    @Test
    public void testYawPitchBehavior() {
        double targetPixelOffsetX = 100;
        double targetPixelOffsetY = 100;
        var targetCenterPoint =
                new Point(imageCenterPoint.x + targetPixelOffsetX, imageCenterPoint.y + targetPixelOffsetY);

        var targetYawPitch =
                TargetCalculations.calculateYawPitch(
                        imageCenterPoint.x,
                        targetCenterPoint.x,
                        params.horizontalFocalLength,
                        imageCenterPoint.y,
                        targetCenterPoint.y,
                        params.verticalFocalLength);

        assertTrue(targetYawPitch.getFirst() > 0, "Yaw is not positive right");
        assertTrue(targetYawPitch.getSecond() < 0, "Pitch is not positive up");

        var fovs =
                FrameStaticProperties.calculateHorizontalVerticalFoV(
                        diagFOV, (int) imageSize.width, (int) imageSize.height);
        var maxYaw =
                TargetCalculations.calculateYawPitch(
                        imageCenterPoint.x,
                        2 * imageCenterPoint.x,
                        params.horizontalFocalLength,
                        imageCenterPoint.y,
                        imageCenterPoint.y,
                        params.verticalFocalLength);
        assertEquals(fovs.getFirst() / 2.0, maxYaw.getFirst(), 0.025, "Horizontal FOV check failed");
        var maxPitch =
                TargetCalculations.calculateYawPitch(
                        imageCenterPoint.x,
                        imageCenterPoint.x,
                        params.horizontalFocalLength,
                        imageCenterPoint.y,
                        0,
                        params.verticalFocalLength);
        assertEquals(fovs.getSecond() / 2.0, maxPitch.getSecond(), 0.025, "Vertical FOV check failed");
    }

    private static Stream<Arguments> testYawPitchCalcArgs() {
        return Stream.of(
                // (yaw, pitch) in degrees
                Arguments.of(0, 0),
                Arguments.of(10, 0),
                Arguments.of(0, 10),
                Arguments.of(10, 10),
                Arguments.of(-10, -10),
                Arguments.of(30, 45),
                Arguments.of(-45, -20));
    }

    private static double[] testCameraMatrix = {240, 0, 320, 0, 240, 320, 0, 0, 1};

    @ParameterizedTest
    @MethodSource("testYawPitchCalcArgs")
    public void testYawPitchCalc(double yawDeg, double pitchDeg) {
        Mat testCameraMat = new Mat(3, 3, CvType.CV_64F);
        testCameraMat.put(0, 0, testCameraMatrix);
        MatOfDouble testDistortion = new MatOfDouble(0.186841202993646,-1.482894102216622,0.005692954661309707,0.0006757267756945662,2.8659664873321287);

        // Since we create this translation using the given yaw/pitch, we should see the same angles
        // calculated
        var targetTrl =
                new Translation3d(1, new Rotation3d(0, Math.toRadians(pitchDeg), Math.toRadians(yawDeg)));
        // NWU to EDN
        var objectPoints =
                new MatOfPoint3f(new Point3(targetTrl.getY(), targetTrl.getZ(), targetTrl.getX()));
        var imagePoints = new MatOfPoint2f();
        // Project translation into camera image
        Calib3d.projectPoints(
                objectPoints,
                new MatOfDouble(0, 0, 0),
                new MatOfDouble(0, 0, 0),
                testCameraMat,
                testDistortion,
                imagePoints);
        var point = imagePoints.toArray()[0];
        // Test if the target yaw/pitch calculation matches what the target was created with
        var yawPitch =
                TargetCalculations.calculateYawPitch(
                        testCameraMatrix[2],
                        point.x,
                        testCameraMatrix[0],
                        testCameraMatrix[5],
                        point.y,
                        testCameraMatrix[4],
                        testCameraMat, testDistortion);
        assertEquals(yawDeg, yawPitch.getFirst(), 1e-3, "Yaw calculation incorrect");
        assertEquals(pitchDeg, yawPitch.getSecond(), 1e-3, "Pitch calculation incorrect");

        testCameraMat.release();
        testDistortion.release();
    }

    @Test
    public void testTargetOffset() {
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
        var gwHorizDeg = glowormHorizVert.getFirst();
        var gwVertDeg = glowormHorizVert.getSecond();
        assertEquals(62.7, gwHorizDeg, .3);
        assertEquals(49, gwVertDeg, .3);
    }

    @Test
    public void testDualOffsetCrosshair() {
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

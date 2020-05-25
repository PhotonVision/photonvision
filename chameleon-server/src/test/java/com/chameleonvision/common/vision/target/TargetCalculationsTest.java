package com.chameleonvision.common.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opencv.core.Point;
import org.opencv.core.Size;

public class TargetCalculationsTest {

    private static Size imageSize = new Size(800, 600);
    private static Point imageCenterPoint = new Point(imageSize.width / 2, imageSize.height / 2);
    private static double CameraHorizontalFocalLength = 61;
    private static double CameraVerticalFocalLength = 34.3;

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
}

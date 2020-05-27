package com.chameleonvision.common.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.chameleonvision.common.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;

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

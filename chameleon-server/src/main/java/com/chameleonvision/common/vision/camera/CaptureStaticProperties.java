package com.chameleonvision.common.vision.camera;

import edu.wpi.cscore.VideoMode;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Point;

public class CaptureStaticProperties {
    public final int imageWidth;
    public final int imageHeight;
    public final double fov;
    public final double imageArea;
    public final double centerX;
    public final double centerY;
    public final Point centerPoint;
    public final double horizontalFocalLength;
    public final double verticalFocalLength;
    public final VideoMode mode;

    public CaptureStaticProperties(VideoMode mode, double fov) {
        this.mode = mode;

        this.imageWidth = mode.width;
        this.imageHeight = mode.height;
        this.fov = fov;

        imageArea = imageHeight * imageWidth;
        centerX = imageWidth / 2.0 - 0.5;
        centerY = imageHeight / 2.0 - 0.5;
        centerPoint = new Point(centerX, centerY);

        // Calculations from pinhole-model.
        double diagonalView = FastMath.toRadians(this.fov);
        Fraction aspectRatio = new Fraction(imageWidth, imageHeight);

        int horizontalRatio = aspectRatio.getNumerator();
        int verticalRatio = aspectRatio.getDenominator();

        double diagonalAspect = FastMath.hypot(horizontalRatio, verticalRatio);

        double horizontalView =
                FastMath.atan(FastMath.tan(diagonalView / 2) * (horizontalRatio / diagonalAspect)) * 2;
        double verticalView =
                FastMath.atan(FastMath.tan(diagonalView / 2) * (verticalRatio / diagonalAspect)) * 2;

        horizontalFocalLength = imageWidth / (2 * FastMath.tan(horizontalView / 2));
        verticalFocalLength = imageHeight / (2 * FastMath.tan(verticalView / 2));
    }
}

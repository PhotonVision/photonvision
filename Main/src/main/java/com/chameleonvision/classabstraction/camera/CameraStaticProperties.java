package com.chameleonvision.classabstraction.camera;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;

public class CameraStaticProperties {

    public final int ImageWidth;
    public final int ImageHeight;
    public final double FOV;
    public final double ImageArea;
    public final double CenterX;
    public final double CenterY;
    public final double HorizontalFocalLength;
    public final double VerticalFocalLength;

    public CameraStaticProperties(int imageWidth, int imageHeight, double fov) {
        ImageWidth = imageWidth;
        ImageHeight = imageHeight;
        FOV = fov;
        ImageArea = ImageWidth * ImageHeight;
        CenterX = ((double) ImageWidth / 2) - 0.5;
        CenterY = ((double) ImageHeight / 2) - 0.5;

        // pinhole model calculations
        double diagonalView = FastMath.toRadians(FOV);
        Fraction aspectFraction = new Fraction(ImageWidth, ImageHeight);
        int horizontalRatio = aspectFraction.getNumerator();
        int verticalRatio = aspectFraction.getDenominator();
        double diagonalAspect = FastMath.hypot(horizontalRatio, verticalRatio);
        double horizontalView = FastMath.atan(FastMath.tan(diagonalView / 2) * (horizontalRatio / diagonalAspect)) * 2;
        double verticalView = FastMath.atan(FastMath.tan(diagonalView / 2) * (verticalRatio / diagonalAspect)) * 2;
        HorizontalFocalLength = ImageWidth / (2 * FastMath.tan(horizontalView /2));
        VerticalFocalLength = ImageHeight / (2 * FastMath.tan(verticalView /2));
    }
}

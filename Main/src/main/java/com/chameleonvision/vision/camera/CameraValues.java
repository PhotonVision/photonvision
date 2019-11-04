package com.chameleonvision.vision.camera;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;

public class CameraValues {
   public final int ImageWidth;
   public final int ImageHeight;
   public final double FOV;
   public final double ImageArea;
   public final double CenterX;
   public final double CenterY;
   private final double HorizontalFocalLength;
   private final double VerticalFocalLength;

   public CameraValues(USBCamera USBCamera) {
       this(USBCamera.getVideoMode().width, USBCamera.getVideoMode().height, USBCamera.getFOV());
   }


    public CameraValues(int imageWidth, int imageHeight, double fov) {
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

    public double CalculatePitch(double PixelY, double centerY) {
       double pitch = FastMath.toDegrees(FastMath.atan((PixelY - centerY) / VerticalFocalLength));
       return (pitch * -1);
    }

    public double CalculateYaw(double PixelX, double centerX) {
        return FastMath.toDegrees(FastMath.atan((PixelX - centerX) / HorizontalFocalLength));
    }
}

package com.chameleonvision.vision;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;

public class CameraValues {
   public final int ImageWidth;
   public final int ImageHeight;
   public final double FOV;
   public final double ImageArea;
   public final double CenterX;
   public final double CenterY;
   public final double DiagonalView;
   public final Fraction AspectFraction;
   public final int HorizontalRatio;
   public final int VerticalRatio;
   public final double HorizontalView;
   public final double VerticalView;
   public final double HorizontalFocalLength;
   public final double VerticalFocalLength;

    public CameraValues(int imageWidth, int imageHeight, double fov) {
        ImageWidth = imageWidth;
        ImageHeight = imageHeight;
        FOV = fov;
        ImageArea = ImageWidth * ImageHeight;
        CenterX = ((double) ImageWidth / 2) - 0.5;
        CenterY = ((double) ImageHeight / 2) - 0.5;
        DiagonalView = FastMath.toRadians(FOV);
        AspectFraction = new Fraction(ImageWidth, ImageHeight);
        HorizontalRatio = AspectFraction.getNumerator();
        VerticalRatio = AspectFraction.getDenominator();
        HorizontalView = FastMath.atan(FastMath.tan(DiagonalView / 2) * (HorizontalRatio / DiagonalView)) * 2;
        VerticalView = FastMath.atan(FastMath.tan(DiagonalView/2) * (VerticalRatio / DiagonalView)) * 2;
        HorizontalFocalLength = ImageWidth / (2 * FastMath.tan(HorizontalView /2));
        VerticalFocalLength = ImageWidth / (2 * FastMath.tan(VerticalView /2));
    }
    public double CalculatePitch(double PixelY, double centerY){
        return (FastMath.toDegrees((FastMath.atan(PixelY - centerY) / VerticalFocalLength)) * -1);
    }
    public double CalculateYaw(double PixelX, double centerX){
        return FastMath.toDegrees(FastMath.atan(PixelX - centerX) / HorizontalFocalLength);
    }
}

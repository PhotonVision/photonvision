package com.chameleonvision.vision.camera;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;

@SuppressWarnings("WeakerAccess")
public class CameraValues {
   public final int ImageWidth;
   public final int ImageHeight;
   public final double FOV;
   public final double ImageArea;
   public final double CenterX;
   public final double CenterY;
   public final double DiagonalView;
   public final double DiagonalAspect;
   public final Fraction AspectFraction;
   public final int HorizontalRatio;
   public final int VerticalRatio;
   public final double HorizontalView;
   public final double VerticalView;
   public final double HorizontalFocalLength;
   public final double VerticalFocalLength;

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
        DiagonalView = FastMath.toRadians(FOV);
        AspectFraction = new Fraction(ImageWidth, ImageHeight);
        HorizontalRatio = AspectFraction.getNumerator();
        VerticalRatio = AspectFraction.getDenominator();
        DiagonalAspect = FastMath.hypot(HorizontalRatio, VerticalRatio);
        HorizontalView = FastMath.atan(FastMath.tan(DiagonalView / 2) * (HorizontalRatio / DiagonalAspect)) * 2;
        VerticalView = FastMath.atan(FastMath.tan(DiagonalView / 2) * (VerticalRatio / DiagonalAspect)) * 2;
        HorizontalFocalLength = ImageWidth / (2 * FastMath.tan(HorizontalView /2));
        VerticalFocalLength = ImageHeight / (2 * FastMath.tan(VerticalView /2));
    }
    public double CalculatePitch(double PixelY, double centerY){
       double pitch =  FastMath.toDegrees(FastMath.atan((PixelY - centerY) / VerticalFocalLength));
       return (pitch * -1);
    }
    public double CalculateYaw(double PixelX, double centerX){
        return FastMath.toDegrees(FastMath.atan((PixelX - centerX) / HorizontalFocalLength));
    }
}

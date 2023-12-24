package org.photonvision.jni;

public class CalibrationHelper {
    public static class CalResult {

    }

    public static native long Create(int width, int height, long overlayMatPtr, double tolerance);
    public static native long Destroy();
    public static native CalResult Detect(long inputImg, long outputImg);
}

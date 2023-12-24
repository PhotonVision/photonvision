package org.photonvision.jni;

public class CalibrationHelper {
    public static class CalResult {

    }

    public static native long Create(int width, int height, long overlayMatPtr, double tolerance);
    public static native long Destroy();
    public static native CalResult Detect(long inputImg, long outputImg);

    public static void main(String[] args) {
        System.load("/home/matt/Documents/GitHub/photonvision/photon-core/build/libs/photoncoreJNI/shared/linuxx86-64/release/libphotoncorejni.so");
        System.out.println(Create(1, 2, 3, 4));
    }
}

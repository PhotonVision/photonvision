package org.photonvision.raspi;

import java.nio.file.Path;

public class PicamJNI {

    static {
       System.load(Path.of("src/main/resources/native/libpicam.so").toAbsolutePath().toString());
    }

    // Everything is static because only one picam is really ever supported
    private static native boolean createCamera();
    private static native boolean destroyCamera();

    private static native boolean setExposure(int exposure);
    private static native boolean setBrightness(int exposure);
    private static native boolean setISO(int iso);
    private static native boolean setRotation(int rotation);
    private static native boolean setVideoMode(int width, int height, int fps);

    private static native boolean grabFrame(long imageNativeObj);
}

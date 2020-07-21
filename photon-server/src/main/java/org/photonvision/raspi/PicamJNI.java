package org.photonvision.raspi;

import java.io.IOException;
import java.nio.file.Path;

public class PicamJNI {

    static {
       System.load(Path.of("src/main/resources/native/libpicam.so").toAbsolutePath().toString());
    }

    public static void main(String[] args) {
        var cam = new PicamJNI();
        cam.setVideoMode(1920, 1080, 30); 
        cam.setExposure(10);
        // cam.createCamera();
    }

    private native boolean createCamera();
    private native boolean destroyCamera();

    private native boolean setExposure(int exposure);
    private native boolean setBrightness(int exposure);
    private native boolean setISO(int iso);
    private native boolean setRotation(int rotation);
    private native boolean setVideoMode(int width, int height, int fps);

    private native boolean grabFrame(long imageNativeObj);
}

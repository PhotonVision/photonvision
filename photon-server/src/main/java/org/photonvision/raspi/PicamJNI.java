package org.photonvision.raspi;

import java.io.IOException;
import java.nio.file.Path;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.photonvision.common.util.TestUtils;

public class PicamJNI {

    static {
       System.load(Path.of("src/main/resources/native/libpicam.so").toAbsolutePath().toString());
       TestUtils.loadLibraries();
    }

    public static void main(String[] args) {
       
        // PicamJNI.setVideoMode(1920, 1080, 30); 
        // PicamJNI.setExposure(10);
        var mat = new Mat(1080, 1920, CvType.CV_8UC1);
        PicamJNI.grabFrame(mat.nativeObj);

        // cam.createCamera();
    }

    private static native boolean createCamera();
    private static native boolean destroyCamera();

    private static native boolean setExposure(int exposure);
    private static native boolean setBrightness(int exposure);
    private static native boolean setISO(int iso);
    private static native boolean setRotation(int rotation);
    private static native boolean setVideoMode(int width, int height, int fps);

    private static native boolean grabFrame(long imageNativeObj);
}

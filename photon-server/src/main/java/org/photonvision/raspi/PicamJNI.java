package org.photonvision.raspi;

import org.photonvision.common.util.TestUtils;

import java.nio.file.Path;

public class PicamJNI {

    static {
       System.load(Path.of("src/main/resources/native/libpicam.so").toAbsolutePath().toString());
    }

    // Everything here is static because only one picam is really ever supported

    /**
     * Called once to make the input texture (held through JOGL) be a pointer (in the GPU) to the data captured by the camera.
     *
     * @param textureId OpenGL texture ID for the input texture used by the fragment shader.
     * @return true on error.
     */
    public static native boolean createImageKHR(int textureId);

    /**
     * Called once for each video mode change. createImageKHR must be called once first. Starts a native thread that sets up OpenMAX and stays alive until destroyCamera is called.
     *
     * @return true on error.
     */
    public static native boolean createCamera(int width, int height, int fps);

    /**
     * Destroys OpenMAX and EGL contexts. Called once for each video mode change *before* createCamera.
     *
     * @return true on error.
     */
    public static native boolean destroyCamera(); // Called before createCamera when video mode changes

    private static native boolean setExposure(int exposure);
    private static native boolean setBrightness(int exposure);
    private static native boolean setISO(int iso);
    private static native boolean setRotation(int rotation);

    public static native void grabFrame(long matPointer);
}

/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.raspi;

import java.nio.file.Path;

public class PicamJNI {

    static {
        System.load(Path.of("src/main/resources/native/libpicam.so").toAbsolutePath().toString());
    }

    // Everything here is static because multiple picams are unsupported at the hardware level

    public static native long initVCSMInfo(int width, int height);

    /**
    * Gives the native code a handle to an EGLImage, which is a texture that's filled with camera
    * data to be processed.
    *
    * @return true on error.
    */
    public static native boolean setEGLImageHandle(long eglImage);

    /**
    * Called once for each video mode change. createImageKHR must be called once first. Starts a
    * native thread that sets up OpenMAX and stays alive until destroyCamera is called.
    *
    * @return true on error.
    */
    public static native boolean createCamera(int width, int height, int fps);

    public static native void waitForOMXFillBufferDone();

    /**
    * Destroys OpenMAX and EGL contexts. Called once for each video mode change *before*
    * createCamera.
    *
    * @return true on error.
    */
    public static native boolean
            destroyCamera(); // Called before createCamera when video mode changes

    private static native boolean setExposure(int exposure);

    private static native boolean setBrightness(int exposure);

    private static native boolean setISO(int iso);

    private static native boolean setRotation(int rotation);

    public static native void grabFrame(long matPointer);
}

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

    /**
    * Called once for each video mode change. Starts a native thread running MMAL that stays alive until destroyCamera is called.
    *
    * @return true on error.
    */
    public static native boolean createCamera(int width, int height, int fps);

    /**
    * Destroys MMAL and EGL contexts. Called once for each video mode change *before*
    * createCamera.
    *
    * @return true on error.
    */
    public static native boolean destroyCamera();

    public static native boolean setThresholds(double hL, double sL, double vL, double hU, double sU, double vU);

    private static native boolean setExposure(int exposure);

    private static native boolean setBrightness(int exposure);

    private static native boolean setISO(int iso);

    private static native boolean setRotation(int rotation);

    public static native long grabFrame();
}

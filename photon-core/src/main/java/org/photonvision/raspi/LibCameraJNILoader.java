/*
 * Copyright (C) Photon Vision.
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

import java.io.File;
import java.io.IOException;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * Helper for extracting photon-libcamera-gl-driver shared library files. TODO: Refactor to use
 * PhotonJNICommon
 */
public class LibCameraJNILoader {
    private static boolean libraryLoaded = false;
    private static final Logger logger = new Logger(LibCameraJNILoader.class, LogGroup.Camera);

    public static synchronized void forceLoad() throws IOException {
        if (libraryLoaded) return;

        var libraryName = "photonlibcamera";

        try {
            // Temp - use the one we built here
            File temp = new File("/home/pi/photon-libcamera-gl-driver/cmake_build/libphotonlibcamera.so");

            System.load(temp.getAbsolutePath());

            logger.info("Successfully loaded shared object " + temp.getName());

        } catch (UnsatisfiedLinkError e) {
            logger.error("Couldn't load shared object " + libraryName, e);
            e.printStackTrace();
        }
        libraryLoaded = true;
    }

    public static boolean isSupported() {
        return libraryLoaded && LibCameraJNI.isSupported();
    }
}

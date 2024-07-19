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
import java.io.FileOutputStream;
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

    private static File extractLibrary(String libraryName) throws IOException{
        // We always extract the shared object (we could hash each so, but that's a lot of work)
        var arch_name = "linuxarm64";
        var nativeLibName = System.mapLibraryName(libraryName);
        var resourcePath = "/nativelibraries/" + arch_name + "/" + nativeLibName;
        var in = LibCameraJNILoader.class.getResourceAsStream(resourcePath);

        if (in == null) {
            logger.error("Failed to find internal native library at path " + resourcePath);
            libraryLoaded = false;
            throw new IOException();
        }

        // It's important that we don't mangle the names of these files on Windows at least
        File temp = new File(System.getProperty("java.io.tmpdir"), nativeLibName);
        FileOutputStream fos = new FileOutputStream(temp);

        int read = -1;
        byte[] buffer = new byte[1024];
        while ((read = in.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
        in.close();

        return temp;
    }

    public static synchronized void forceLoad() throws IOException {
        if (libraryLoaded) return;

        var libraryName = "photonlibcamera";

        try {
            // Development aid. First, try to load the library from disc if it was built locally.
            File temp = new File("/home/pi/photon-libcamera-gl-driver/cmake_build/libphotonlibcamera.so");

            if(!temp.exists()){
                // File was not already on disc. Extract it from the jar.
                temp = extractLibrary(libraryName);
            }

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

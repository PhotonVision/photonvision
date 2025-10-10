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

import java.io.IOException;
import java.util.List;
import org.photonvision.jni.PhotonJNICommon;

/** Helper for extracting photon-libcamera-gl-driver shared library files. */
public class LibCameraJNILoader extends PhotonJNICommon {
    private boolean libraryLoaded = false;
    private static LibCameraJNILoader instance = null;

    public static synchronized LibCameraJNILoader getInstance() {
        if (instance == null) instance = new LibCameraJNILoader();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        forceLoad(
                LibCameraJNILoader.getInstance(), LibCameraJNILoader.class, List.of("photonlibcamera"));
    }

    @Override
    public boolean isLoaded() {
        return libraryLoaded;
    }

    @Override
    public void setLoaded(boolean state) {
        libraryLoaded = state;
    }

    public boolean isSupported() {
        return libraryLoaded && LibCameraJNI.isSupported();
    }
}

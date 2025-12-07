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

import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;

/** Helper for extracting photon-libcamera-gl-driver shared library files. */
public class LibCameraJNILoader {
    private boolean libraryLoaded = false;
    private static LibCameraJNILoader instance = null;

    public static synchronized LibCameraJNILoader getInstance() {
        if (instance == null) instance = new LibCameraJNILoader();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        CombinedRuntimeLoader.loadLibraries(LibCameraJNILoader.class, "photonlibcamera");
    }

    public boolean isLoaded() {
        return libraryLoaded;
    }

    public void setLoaded(boolean state) {
        libraryLoaded = state;
    }

    public boolean isSupported() {
        return libraryLoaded && LibCameraJNI.isSupported();
    }
}

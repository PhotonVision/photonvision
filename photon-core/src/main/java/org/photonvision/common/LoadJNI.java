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

package org.photonvision.common;

import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;
import java.util.HashMap;
import org.photonvision.jni.LibraryLoader;

public class LoadJNI {
    private static HashMap<JNITypes, Boolean> loadedMap = new HashMap<>();

    public enum JNITypes {
        RUBIK_DETECTOR("tensorflowlite", "tensorflowlite_c", "external_delegate", "rubik_jni"),
        RKNN_DETECTOR("rga", "rknnrt", "rknn_jni"),
        MRCAL("mrcal_jni"),
        LIBCAMERA("photonlibcamera");

        public final String[] libraries;

        JNITypes(String... libraries) {
            this.libraries = libraries;
        }
    }

    public static synchronized void forceLoad(JNITypes type) throws IOException {
        loadLibraries();

        if (loadedMap.getOrDefault(type, false)) {
            return;
        }

        CombinedRuntimeLoader.loadLibraries(LoadJNI.class, type.libraries);
        loadedMap.put(type, true);
    }

    public static boolean loadLibraries() {
        return LibraryLoader.loadWpiLibraries() && LibraryLoader.loadTargeting();
    }

    public static boolean hasLoaded(JNITypes t) {
        return loadedMap.getOrDefault(t, false);
    }
}

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
import org.photonvision.jni.LibraryLoader;

public class LoadJNI {
    public enum JNITypes {
        RUBIK_DETECTOR(false, "tensorflowlite", "tensorflowlite_c", "external_delegate", "rubik_jni"),
        RKNN_DETECTOR(false, "rga", "rknnrt", "rknn_jni"),
        MRCAL(false, "mrcal_jni"),
        LIBCAMERA(false, "photonlibcamera");

        private volatile boolean hasLoaded;
        public final String[] libraries;

        JNITypes(boolean hasLoaded, String... libraries) {
            this.hasLoaded = hasLoaded;
            this.libraries = libraries;
        }

        public boolean hasLoaded() {
            return hasLoaded;
        }

        public void setHasLoaded(boolean loaded) {
            this.hasLoaded = loaded;
        }
    }

    public static synchronized void forceLoad(JNITypes type) throws IOException {
        loadLibraries();

        if (type.hasLoaded()) {
            return;
        }

        CombinedRuntimeLoader.loadLibraries(LoadJNI.class, type.libraries);
        type.setHasLoaded(true);
    }

    public static boolean loadLibraries() {
        return LibraryLoader.loadWpiLibraries() && LibraryLoader.loadTargeting();
    }
}

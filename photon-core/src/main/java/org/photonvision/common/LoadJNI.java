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
        RUBIK_DETECTOR(false),
        RKNN_DETECTOR(false),
        MRCAL(false),
        LIBCAMERA(false);

        private volatile boolean hasLoaded;

        JNITypes(boolean hasLoaded) {
            this.hasLoaded = hasLoaded;
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

        switch (type) {
            case RUBIK_DETECTOR:
                CombinedRuntimeLoader.loadLibraries(
                        LoadJNI.class, "tensorflowlite", "tensorflowlite_c", "external_delegate", "rubik_jni");
                type.setHasLoaded(true);
                break;
            case RKNN_DETECTOR:
                CombinedRuntimeLoader.loadLibraries(LoadJNI.class, "rga", "rknnrt", "rknn_jni");
                type.setHasLoaded(true);
                break;
            case MRCAL:
                CombinedRuntimeLoader.loadLibraries(LoadJNI.class, "mrcal_jni");
                type.setHasLoaded(true);
                break;
            case LIBCAMERA:
                CombinedRuntimeLoader.loadLibraries(LoadJNI.class, "photonlibcamera");
                type.setHasLoaded(true);
                break;
            default:
                throw new IOException("Unknown JNI type: " + type);
        }
    }

    public static boolean loadLibraries() {
        return LibraryLoader.loadWpiLibraries() && LibraryLoader.loadTargeting();
    }
}

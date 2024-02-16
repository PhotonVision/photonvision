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

package org.photonvision.mrcal;

import java.io.IOException;
import java.util.List;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.util.TestUtils;
import org.photonvision.jni.PhotonJNICommon;

public class MrCalJNILoader extends PhotonJNICommon {
    private boolean isLoaded;
    private static MrCalJNILoader instance = null;

    private MrCalJNILoader() {
        isLoaded = false;
    }

    public static synchronized MrCalJNILoader getInstance() {
        if (instance == null) instance = new MrCalJNILoader();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        // Force load opencv
        TestUtils.loadLibraries();

        // Library naming is dumb and has "lib" appended for Windows when it ought not to
        if (Platform.isWindows()) {
            // Order is correct to match dependencies of libraries
            forceLoad(
                    MrCalJNILoader.getInstance(),
                    MrCalJNILoader.class,
                    List.of(
                            "libamd",
                            "libcamd",
                            "libcolamd",
                            "libccolamd",
                            "openblas",
                            "libwinpthread-1",
                            "libgcc_s_seh-1",
                            "libquadmath-0",
                            "libgfortran-5",
                            "liblapack",
                            "libcholmod",
                            "mrcal_jni"));
        } else {
            // Nothing else to do on linux
            forceLoad(MrCalJNILoader.getInstance(), MrCalJNILoader.class, List.of("mrcal_jni"));
        }

        if (!MrCalJNILoader.getInstance().isLoaded()) {
            throw new IOException("Unable to load mrcal JNI!");
        }
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void setLoaded(boolean state) {
        isLoaded = state;
    }
}

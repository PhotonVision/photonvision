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

package org.photonvision.jni;

import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;
import org.photonvision.common.util.TestUtils;

public class RubikDetectorJNI {
    private boolean isLoaded;
    private static RubikDetectorJNI instance = null;

    private RubikDetectorJNI() {
        isLoaded = false;
    }

    public static RubikDetectorJNI getInstance() {
        if (instance == null) instance = new RubikDetectorJNI();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();

        CombinedRuntimeLoader.loadLibraries(
                RubikDetectorJNI.class,
                "tensorflowlite",
                "tensorflowlite_c",
                "external_delegate",
                "rubik_jni");
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean state) {
        isLoaded = state;
    }
}

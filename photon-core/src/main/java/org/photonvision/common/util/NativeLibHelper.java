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

package org.photonvision.common.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NativeLibHelper {
    private static NativeLibHelper INSTANCE;

    public static NativeLibHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NativeLibHelper();
        }

        return INSTANCE;
    }

    public final Path NativeLibPath;

    private NativeLibHelper() {
        String home = System.getProperty("user.home");
        NativeLibPath = Paths.get(home, ".pvlibs", "nativecache");
    }
}

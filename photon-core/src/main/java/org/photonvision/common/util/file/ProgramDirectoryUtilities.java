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

package org.photonvision.common.util.file;

import java.io.File;
import java.net.URISyntaxException;

public class ProgramDirectoryUtilities {
    private static String getJarName() {
        return new File(
                        ProgramDirectoryUtilities.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .getPath())
                .getName();
    }

    private static boolean runningFromJAR() {
        String jarName = getJarName();
        return jarName.contains(".jar");
    }

    public static String getProgramDirectory() {
        if (runningFromJAR()) {
            return getCurrentJARDirectory();
        } else {
            return System.getProperty("user.dir");
        }
    }

    private static String getCurrentJARDirectory() {
        try {
            return new File(
                            ProgramDirectoryUtilities.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI()
                                    .getPath())
                    .getParent();
        } catch (URISyntaxException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}

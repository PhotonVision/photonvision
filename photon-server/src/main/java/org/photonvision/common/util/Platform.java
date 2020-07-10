/*
 * Copyright (C) 2020 Photon Vision.
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

import edu.wpi.first.wpiutil.RuntimeDetector;
import java.io.IOException;

@SuppressWarnings("unused")
public enum Platform {
    WINDOWS_32("Windows x32"),
    WINDOWS_64("Windows x64"),
    LINUX_32("Linux x32"),
    LINUX_64("Linux x64"),
    LINUX_RASPBIAN("Linux Raspbian"), // TODO: check that RaspiOS reports the same way
    LINUX_AARCH64BIONIC("Linux Aarch64 Bionic"),
    UNSUPPORTED(UnsupportedPlatformString);

    public final String value;
    public final boolean isRoot = checkForRoot();

    Platform(String value) {
        this.value = value;
    }

    private static String UnsupportedPlatformString =
            "Unsupported Platform - OS: " + OS_NAME + ", Architecture: " + OS_ARCH;

    public static final Platform CurrentPlatform = getCurrentPlatform();

    public static boolean isWindows() {
        return CurrentPlatform == WINDOWS_64 || CurrentPlatform == WINDOWS_32;
    }

    public static boolean isLinux() {
        return CurrentPlatform != UNSUPPORTED && !isWindows();
    }

    public static boolean isRaspberryPi() {
        return CurrentPlatform.equals(LINUX_RASPBIAN);
    }

    private static ShellExec shell = new ShellExec(true, false);

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean checkForRoot() {
        if (isLinux()) {
            try {
                shell.execute("id", null, true, "-u");
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                return shell.getOutput().split("\n")[0].equals("0");
            }

        } else {
            return true;
        }
        return false;
    }

    private static Platform getCurrentPlatform() {
        if (RuntimeDetector.isWindows()) {
            if (RuntimeDetector.is32BitIntel()) return WINDOWS_32;
            if (RuntimeDetector.is64BitIntel()) return WINDOWS_64;
        }

        if (RuntimeDetector.isLinux()) {
            if (RuntimeDetector.is32BitIntel()) return LINUX_32;
            if (RuntimeDetector.is64BitIntel()) return LINUX_64;
            if (RuntimeDetector.isRaspbian()) return LINUX_RASPBIAN;
            if (RuntimeDetector.isAarch64Bionic()) return LINUX_AARCH64BIONIC;
        }

        return UNSUPPORTED;
    }

    public String toString() {
        return this.value;
    }
}

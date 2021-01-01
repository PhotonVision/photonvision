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

package org.photonvision.common.hardware;

import edu.wpi.first.wpiutil.RuntimeDetector;
import java.io.IOException;
import org.photonvision.common.util.ShellExec;

@SuppressWarnings("unused")
public enum Platform {
    // WPILib Supported (JNI)
    WINDOWS_32("Windows x32"),
    WINDOWS_64("Windows x64"),
    LINUX_64("Linux x64"),
    LINUX_RASPBIAN("Linux Raspbian"), // Raspberry Pi 3/4
    LINUX_AARCH64BIONIC("Linux AARCH64 Bionic"), // Jetson Nano, Jetson TX2
    // PhotonVision Supported (Manual install)
    LINUX_ARM32("Linux ARM32"), // ODROID XU4, C1+
    LINUX_ARM64("Linux ARM64"), // ODROID C2, N2

    // Completely unsupported
    UNSUPPORTED("Unsupported Platform");

    private static final ShellExec shell = new ShellExec(true, false);
    public final String value;
    public static final boolean isRoot = checkForRoot();

    Platform(String value) {
        this.value = value;
    }

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    public static final Platform CurrentPlatform = getCurrentPlatform();

    private static String UnknownPlatformString =
            String.format("Unknown Platform. OS: %s, Architecture: %s", OS_NAME, OS_ARCH);

    public boolean isWindows() {
        return this == WINDOWS_64 || this == WINDOWS_32;
    }

    public static boolean isLinux() {
        return getCurrentPlatform() == LINUX_64
                || getCurrentPlatform() == LINUX_RASPBIAN
                || getCurrentPlatform() == LINUX_ARM64;
    }

    public static boolean isRaspberryPi() {
        return CurrentPlatform.equals(LINUX_RASPBIAN);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static boolean checkForRoot() {
        if (isLinux()) {
            try {
                shell.executeBashCommand("id -u");
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

    public static Platform getCurrentPlatform() {
        if (RuntimeDetector.isWindows()) {
            if (RuntimeDetector.is32BitIntel()) return WINDOWS_32;
            if (RuntimeDetector.is64BitIntel()) return WINDOWS_64;
        }

        if (RuntimeDetector.isMac()) {
            if (RuntimeDetector.is32BitIntel()) return UNSUPPORTED;
        }

        if (RuntimeDetector.isLinux()) {
            if (RuntimeDetector.is32BitIntel()) return UNSUPPORTED;
            if (RuntimeDetector.is64BitIntel()) return LINUX_64;
            if (RuntimeDetector.isRaspbian()) return LINUX_RASPBIAN;
        }

        System.out.println(UnknownPlatformString);
        return Platform.UNSUPPORTED;
    }

    public String toString() {
        if (this.equals(UNSUPPORTED)) {
            return UnknownPlatformString;
        } else {
            return this.value;
        }
    }
}

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

import com.jogamp.common.os.Platform.OSType;
import edu.wpi.first.util.RuntimeDetector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.photonvision.common.util.ShellExec;

@SuppressWarnings("unused")
public enum Platform {
    // WPILib Supported (JNI)
    WINDOWS_64("Windows x64", false, OSType.WINDOWS, true),
    LINUX_32("Linux x86", false, OSType.LINUX, true),
    LINUX_64("Linux x64", false, OSType.LINUX, true),
    LINUX_RASPBIAN32(
            "Linux Raspbian 32-bit", true, OSType.LINUX, true), // Raspberry Pi 3/4 with a 32-bit image
    LINUX_RASPBIAN64(
            "Linux Raspbian 64-bit", true, OSType.LINUX, true), // Raspberry Pi 3/4 with a 64-bit image
    LINUX_AARCH64("Linux AARCH64", false, OSType.LINUX, true), // Jetson Nano, Jetson TX2

    // PhotonVision Supported (Manual build/install)
    LINUX_ARM32("Linux ARM32", false, OSType.LINUX, true), // ODROID XU4, C1+
    LINUX_ARM64("Linux ARM64", false, OSType.LINUX, true), // ODROID C2, N2

    // Completely unsupported
    WINDOWS_32("Windows x86", false, OSType.WINDOWS, false),
    MACOS("Mac OS", false, OSType.MACOS, false),
    UNKNOWN("Unsupported Platform", false, OSType.UNKNOWN, false);

    private enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    private static final ShellExec shell = new ShellExec(true, false);
    public final String description;
    public final boolean isPi;
    public final OSType osType;
    public final boolean isSupported;

    // Set once at init, shouldn't be needed after.
    private static final Platform currentPlatform = getCurrentPlatform();
    private static final boolean isRoot = checkForRoot();

    Platform(String description, boolean isPi, OSType osType, boolean isSupported) {
        this.description = description;
        this.isPi = isPi;
        this.osType = osType;
        this.isSupported = isSupported;
    }

    //////////////////////////////////////////////////////
    // Public API

    // Checks specifically if unix shell and API are supported
    public static boolean isLinux() {
        return currentPlatform.osType == OSType.LINUX;
    }

    public static boolean isRaspberryPi() {
        return currentPlatform.isPi;
    }

    public static String getPlatformName() {
        if (currentPlatform.equals(UNKNOWN)) {
            return UnknownPlatformString;
        } else {
            return currentPlatform.description;
        }
    }

    public static boolean isRoot() {
        return isRoot;
    }

    //////////////////////////////////////////////////////

    // Debug info related to unknown platforms for debug help
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String UnknownPlatformString =
            String.format("Unknown Platform. OS: %s, Architecture: %s", OS_NAME, OS_ARCH);

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

    private static Platform getCurrentPlatform() {
        if (RuntimeDetector.isWindows()) {
            if (RuntimeDetector.is32BitIntel()) {
                return WINDOWS_32;
            } else if (RuntimeDetector.is64BitIntel()) {
                return WINDOWS_64;
            } else {
                // please don't try this
                return UNKNOWN;
            }
        }

        if (RuntimeDetector.isMac()) {
            // TODO - once we have real support, this might have to be more granular
            return MACOS;
        }

        if (RuntimeDetector.isLinux()) {
            if (isPiSBC()) {
                if (RuntimeDetector.isArm32()) {
                    return LINUX_RASPBIAN32;
                } else if (RuntimeDetector.isArm64()) {
                    return LINUX_RASPBIAN64;
                } else {
                    // Unknown/exotic installation
                    return UNKNOWN;
                }
            } else if (isJetsonSBC()) {
                if (RuntimeDetector.isArm64()) {
                    // TODO - do we need to check OS version?
                    return LINUX_AARCH64;
                } else {
                    // Unknown/exotic installation
                    return UNKNOWN;
                }
            } else if (RuntimeDetector.is64BitIntel()) {
                return LINUX_64;
            } else if (RuntimeDetector.is32BitIntel()) {
                return LINUX_32;
            } else if (RuntimeDetector.isArm64()) {
                // TODO - os detection needed?
                return LINUX_AARCH64;
            } else {
                // Unknown or otherwise unsupported platform
                return Platform.UNKNOWN;
            }
        }

        // If we fall through all the way to here,
        return Platform.UNKNOWN;
    }

    // Check for various known SBC types
    private static boolean isPiSBC() {
        return fileHasText("/proc/cpuinfo", "Raspberry Pi");
    }

    private static boolean isJetsonSBC() {
        // https://forums.developer.nvidia.com/t/how-to-recognize-jetson-nano-device/146624
        return fileHasText("/proc/device-tree/model", "NVIDIA Jetson");
    }

    // Checks for various names of linux OS
    private static boolean isStretch() {
        // TODO - this is a total guess
        return fileHasText("/etc/os-release", "Stretch");
    }

    private static boolean isBuster() {
        // TODO - this is a total guess
        return fileHasText("/etc/os-release", "Buster");
    }

    private static boolean fileHasText(String filename, String text) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            while (true) {
                String value = reader.readLine();
                if (value == null) {
                    return false;

                } else if (value.contains(text)) {
                    return true;
                } // else, next line
            }
        } catch (IOException ex) {
            return false;
        }
    }
}

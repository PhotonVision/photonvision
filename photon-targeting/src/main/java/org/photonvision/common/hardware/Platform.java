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

import edu.wpi.first.util.RuntimeDetector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class Platform {

    // WPILib Supported (JNI)
    public static Platform WINDOWS_64 = new Platform("Windows x64", "winx64", false, OSType.WINDOWS, true);
    public static Platform LINUX_32 = new Platform("Linux x86", "linuxx64", false, OSType.LINUX, true);
    public static Platform LINUX_64 = new Platform("Linux x64", "linuxx64", false, OSType.LINUX, true);
    public static Platform LINUX_RASPBIAN32 = new Platform(
            "Linux Raspbian 32-bit",
            "linuxarm32",
            true,
            OSType.LINUX,
            true); // Raspberry Pi 3/4 with a 32-bit image
    public static Platform LINUX_RASPBIAN64 = new Platform(
            "Linux Raspbian 64-bit",
            "linuxarm64",
            true,
            OSType.LINUX,
            true); // Raspberry Pi 3/4 with a 64-bit image
    public static Platform LINUX_RK3588_64 = new Platform("Linux AARCH 64-bit with RK3588", "linuxarm64", false,
            OSType.LINUX, true);
    public static Platform LINUX_AARCH64 = new Platform(
            "Linux AARCH64", "linuxarm64", false, OSType.LINUX, true); // Jetson Nano, Jetson TX2

    // PhotonVision Supported (Manual build/install)
    public static Platform LINUX_ARM64 = new Platform("Linux ARM64", "linuxarm64", false, OSType.LINUX, true); // ODROID
                                                                                                               // C2, N2

    // Completely unsupported
    public static Platform WINDOWS_32 = new Platform("Windows x86", "windowsx64", false, OSType.WINDOWS, false);
    public static Platform MACOS = new Platform("Mac OS", "osxuniversal", false, OSType.MACOS, false);
    public static Platform LINUX_ARM32 = new Platform("Linux ARM32", "linuxarm32", false, OSType.LINUX, false); // ODROID
                                                                                                                // XU4,
                                                                                                                // C1+
    public static Platform UNKNOWN = new Platform("Unsupported Platform", "", false, OSType.UNKNOWN, false);

    private enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public final String description;
    public final String nativeLibraryFolderName;
    public final boolean isPi;
    public final OSType osType;
    public final boolean isSupported;

    private final boolean isPiSBC;
    private final boolean isJetsonSBC;
    private final boolean isOrangePi;
    private final boolean isCoolPi4b;
    private final boolean isStretch;
    private final boolean isBuster;
    private final boolean isRK3588;

    // Set once at init, shouldn't be needed after.
    private static final Platform currentPlatform = getCurrentPlatform();

    Platform(
            String description,
            String nativeLibFolderName,
            boolean isPi,
            OSType osType,
            boolean isSupported) {
        this.description = description;
        this.isPi = isPi;
        this.osType = osType;
        this.isSupported = isSupported;
        this.nativeLibraryFolderName = nativeLibFolderName;

        this.isPiSBC = fileHasText("/proc/cpuinfo", "Raspberry Pi");
        this.isJetsonSBC = fileHasText("/proc/device-tree/model", "NVIDIA Jetson");
        this.isOrangePi = fileHasText("/proc/device-tree/model", "Orange Pi 5");
        this.isCoolPi4b = fileHasText("/proc/device-tree/model", "CoolPi 4B");
        this.isStretch = fileHasText("/etc/os-release", "Stretch");
        this.isBuster = fileHasText("/etc/os-release", "Buster");
        this.isRK3588 = isOrangePi || isCoolPi4b;

    }

    public Platform(
            String description,
            String nativeLibFolderName,
            boolean isPi,
            OSType osType,
            boolean isSupported,
            boolean isPiSBC,
            boolean isJetsonSBC,
            boolean isOrangePi,
            boolean isCoolPi4b,
            boolean isStretch,
            boolean isBuster,
            boolean isRK3588) {
        this.description = description;
        this.isPi = isPi;
        this.osType = osType;
        this.isSupported = isSupported;
        this.nativeLibraryFolderName = nativeLibFolderName;

        this.isPiSBC = isPiSBC;
        this.isJetsonSBC = isJetsonSBC;
        this.isOrangePi = isOrangePi;
        this.isCoolPi4b = isCoolPi4b;
        this.isStretch = isStretch;
        this.isBuster = isBuster;
        this.isRK3588 = isRK3588;
    }

    //////////////////////////////////////////////////////
    // Public API

    // Checks specifically if unix shell and API are supported
    public boolean isLinux() {
        return osType == OSType.LINUX;
    }

    public boolean isRK3588() {
        return isOrangePi() || isCoolPi4b();
    }

    public boolean isRaspberryPi() {
        return currentPlatform.isPi;
    }

    public static String getPlatformName() {
        if (currentPlatform.equals(UNKNOWN)) {
            return UnknownPlatformString;
        } else {
            return currentPlatform.description;
        }
    }

    public static String getNativeLibraryFolderName() {
        return currentPlatform.nativeLibraryFolderName;
    }

    public static boolean isSupported() {
        return currentPlatform.isSupported;
    }

    //////////////////////////////////////////////////////

    // Debug info related to unknown platforms for debug help
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String UnknownPlatformString = String.format("Unknown Platform. OS: %s, Architecture: %s",
            OS_NAME, OS_ARCH);

<<<<<<< Updated upstream
    private static Platform getCurrentPlatform() {
=======
    public static Platform getCurrentPlatform() {

        if (currentPlatform != null) {
            return currentPlatform;
        }

>>>>>>> Stashed changes
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
            if (isCurrentPiSBC()) {
                if (RuntimeDetector.isArm32()) {
                    return LINUX_RASPBIAN32;
                } else if (RuntimeDetector.isArm64()) {
                    return LINUX_RASPBIAN64;
                } else {
                    // Unknown/exotic installation
                    return UNKNOWN;
                }
            } else if (isCurrentJetsonSBC()) {
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
                if (isCurrentOrangePi()) {
                    return LINUX_RK3588_64;
                } else {
                    return LINUX_AARCH64;
                }
            } else if (RuntimeDetector.isArm32()) {
                return LINUX_ARM32;
            } else {
                // Unknown or otherwise unsupported platform
                return Platform.UNKNOWN;
            }
        }

        // If we fall through all the way to here,
        return Platform.UNKNOWN;
    }

    // Check for various known SBC types
    private static boolean isCurrentPiSBC() {
        return fileHasText("/proc/cpuinfo", "Raspberry Pi");
    }

    public boolean isPiSBC() {
        return isPiSBC;
    }

    private static boolean isCurrentOrangePi() {
        return fileHasText("/proc/device-tree/model", "Orange Pi 5");
    }

    public boolean isOrangePi() {
        return isOrangePi;
    }

    public boolean isCoolPi4b() {
        return isCoolPi4b;
    }

    public boolean isJetsonSBC() {
        return isJetsonSBC;
    }

    private static boolean isCurrentJetsonSBC() {
        return fileHasText("/proc/device-tree/model", "NVIDIA Jetson");
    }

    public boolean isStretch() {
        return isStretch;
    }

    public boolean isBuster() {
        return isBuster;
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

    public boolean isWindows() {
        return (osType == OSType.WINDOWS);
    }
}

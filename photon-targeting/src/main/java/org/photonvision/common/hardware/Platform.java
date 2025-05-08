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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Platform {
    // WPILib Supported (JNI)
    WINDOWS_64("Windows x64", Platform::getUnknownModel, "winx64", false, OSType.WINDOWS, true),
    LINUX_32("Linux x86", Platform::getUnknownModel, "linuxx64", false, OSType.LINUX, true),
    LINUX_64("Linux x64", Platform::getUnknownModel, "linuxx64", false, OSType.LINUX, true),
    LINUX_RASPBIAN32(
            "Linux Raspbian 32-bit",
            Platform::getLinuxDeviceTreeModel,
            "linuxarm32",
            true,
            OSType.LINUX,
            true), // Raspberry Pi 3/4 with a 32-bit image
    LINUX_RASPBIAN64(
            "Linux Raspbian 64-bit",
            Platform::getLinuxDeviceTreeModel,
            "linuxarm64",
            true,
            OSType.LINUX,
            true), // Raspberry Pi 3/4 with a 64-bit image
    LINUX_RK3588_64(
            "Linux AARCH 64-bit with RK3588",
            Platform::getLinuxDeviceTreeModel,
            "linuxarm64",
            false,
            OSType.LINUX,
            true),
    LINUX_AARCH64(
            "Linux AARCH64",
            Platform::getLinuxDeviceTreeModel,
            "linuxarm64",
            false,
            OSType.LINUX,
            true), // Jetson Nano, Jetson TX2

    // PhotonVision Supported (Manual build/install)
    LINUX_ARM64(
            "Linux ARM64",
            Platform::getLinuxDeviceTreeModel,
            "linuxarm64",
            false,
            OSType.LINUX,
            true), // ODROID C2, N2

    // Completely unsupported
    WINDOWS_32("Windows x86", Platform::getUnknownModel, "windowsx64", false, OSType.WINDOWS, false),
    MACOS("Mac OS", Platform::getUnknownModel, "osxuniversal", false, OSType.MACOS, false),
    LINUX_ARM32(
            "Linux ARM32",
            Platform::getUnknownModel,
            "linuxarm32",
            false,
            OSType.LINUX,
            false), // ODROID XU4, C1+
    UNKNOWN("Unsupported Platform", Platform::getUnknownModel, "", false, OSType.UNKNOWN, false);

    public enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public final String description;
    public final String hardwareModel;
    public final String nativeLibraryFolderName;
    public final boolean isPi;
    public final OSType osType;
    public final boolean isSupported;

    // Set once at init, shouldn't be needed after.
    private static final Platform currentPlatform = getCurrentPlatform();

    Platform(
            String description,
            Supplier<String> getHardwareModel,
            String nativeLibFolderName,
            boolean isPi,
            OSType osType,
            boolean isSupported) {
        this.description = description;
        this.hardwareModel = getHardwareModel.get();
        this.isPi = isPi;
        this.osType = osType;
        this.isSupported = isSupported;
        this.nativeLibraryFolderName = nativeLibFolderName;
    }

    //////////////////////////////////////////////////////
    // Public API

    // Checks specifically if unix shell and API are supported
    public static boolean isLinux() {
        return currentPlatform.osType == OSType.LINUX;
    }

    public static boolean isRK3588() {
        return Platform.isOrangePi() || Platform.isCoolPi4b() || Platform.isRock5C();
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

    public static String getHardwareModel() {
        return currentPlatform.hardwareModel;
    }

    public static String getNativeLibraryFolderName() {
        return currentPlatform.nativeLibraryFolderName;
    }

    public static boolean isSupported() {
        return currentPlatform.isSupported;
    }

    public static boolean isAthena() {
        File runRobotFile = new File("/usr/local/frc/bin/frcRunRobot.sh");
        return runRobotFile.exists();
    }

    //////////////////////////////////////////////////////

    // Debug info related to unknown platforms for debug help
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String UnknownPlatformString =
            String.format("Unknown Platform. OS: %s, Architecture: %s", OS_NAME, OS_ARCH);
    private static final String UnknownDeviceModelString = "Unknown";

    public static Platform getCurrentPlatform() {
        String OS_NAME;
        if (Platform.OS_NAME != null) {
            OS_NAME = Platform.OS_NAME;
        } else {
            OS_NAME = System.getProperty("os.name");
        }

        String OS_ARCH;
        if (Platform.OS_ARCH != null) {
            OS_ARCH = Platform.OS_ARCH;
        } else {
            OS_ARCH = System.getProperty("os.arch");
        }

        if (OS_NAME.startsWith("Windows")) {
            if (OS_ARCH.equals("x86") || OS_ARCH.equals("i386")) {
                return WINDOWS_32;
            } else if (OS_ARCH.equals("amd64") || OS_ARCH.equals("x86_64")) {
                return WINDOWS_64;
            } else {
                // please don't try this
                return UNKNOWN;
            }
        }

        if (OS_NAME.startsWith("Mac")) {
            // TODO - once we have real support, this might have to be more granular
            return MACOS;
        }

        if (OS_NAME.startsWith("Linux")) {
            if (isPiSBC()) {
                if (OS_ARCH.equals("arm") || OS_ARCH.equals("arm32")) {
                    return LINUX_RASPBIAN32;
                } else if (OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64")) {
                    return LINUX_RASPBIAN64;
                } else {
                    // Unknown/exotic installation
                    return UNKNOWN;
                }
            } else if (isJetsonSBC()) {
                if (OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64")) {
                    // TODO - do we need to check OS version?
                    return LINUX_AARCH64;
                } else {
                    // Unknown/exotic installation
                    return UNKNOWN;
                }
            } else if (OS_ARCH.equals("amd64") || OS_ARCH.equals("x86_64")) {
                return LINUX_64;
            } else if (OS_ARCH.equals("x86") || OS_ARCH.equals("i386")) {
                return LINUX_32;
            } else if (OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64")) {
                // TODO - os detection needed?
                if (isRK3588()) {
                    return LINUX_RK3588_64;
                } else {
                    return LINUX_AARCH64;
                }
            } else if (OS_ARCH.equals("arm") || OS_ARCH.equals("arm32")) {
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
    private static boolean isPiSBC() {
        return fileHasText("/proc/cpuinfo", "Raspberry Pi");
    }

    private static boolean isOrangePi() {
        return fileHasText("/proc/device-tree/model", "Orange Pi 5");
    }

    private static boolean isRock5C() {
        return fileHasText("/proc/device-tree/model", "ROCK 5C");
    }

    private static boolean isCoolPi4b() {
        return fileHasText("/proc/device-tree/model", "CoolPi 4B");
    }

    private static boolean isJetsonSBC() {
        // https://forums.developer.nvidia.com/t/how-to-recognize-jetson-nano-device/146624
        return fileHasText("/proc/device-tree/model", "NVIDIA Jetson");
    }

    static String getLinuxDeviceTreeModel() {
        var deviceTreeModelPath = Paths.get("/proc/device-tree/model");
        try {
            if (Files.exists(deviceTreeModelPath)) {
                return Files.readString(deviceTreeModelPath).trim();
            }
        } catch (Exception ex) {
            return UnknownDeviceModelString;
        }
        return UnknownDeviceModelString;
    }

    static String getUnknownModel() {
        return UnknownDeviceModelString;
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

    public static boolean isWindows() {
        var p = getCurrentPlatform();
        return (p == WINDOWS_32 || p == WINDOWS_64);
    }
}

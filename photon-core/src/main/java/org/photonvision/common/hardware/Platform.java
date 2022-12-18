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

    // These are queried on init and should never change after
    public static final Platform currentPlatform = getCurrentPlatform();
    static final String currentPiVersionStr = getPiVersionString();
    public static final PiVersion currentPiVersion = PiVersion.getPiVersion();

    private static final String UnknownPlatformString =
            String.format("Unknown Platform. OS: %s, Architecture: %s", OS_NAME, OS_ARCH);

    public static boolean isWindows() {
        return currentPlatform == WINDOWS_64 || currentPlatform == WINDOWS_32;
    }

    public static boolean isLinux() {
        return currentPlatform == LINUX_64
                || currentPlatform == LINUX_RASPBIAN
                || currentPlatform == LINUX_ARM64;
    }

    public static boolean isRaspberryPi() {
        return currentPlatform.equals(LINUX_RASPBIAN);
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
            if (isRaspbian()) return LINUX_RASPBIAN;
            if (RuntimeDetector.is32BitIntel()) return UNSUPPORTED;
            if (RuntimeDetector.is64BitIntel()) return LINUX_64;
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

    // Querry /proc/device-tree/model. This should return the model of the pi
    // Versions here:
    // https://github.com/raspberrypi/linux/blob/rpi-5.10.y/arch/arm/boot/dts/bcm2710-rpi-cm3.dts
    private static String getPiVersionString() {
        if (!isRaspberryPi()) return "";
        try {
            shell.executeBashCommand("cat /proc/device-tree/model");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (shell.getExitCode() == 0) {
            // We expect it to be in the format "raspberry pi X model X"
            return shell.getOutput();
        }

        return "";
    }

    // Depending on OS release, there's a couple ways we might happen to be on a Pi.
    // Check multiple files for a best-guess at what hardware we're on.
    private static boolean isRaspbian() {
        return fileHasText("/etc/os-release", "Raspbian")
                || fileHasText("/proc/cpuinfo", "Raspberry Pi");
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

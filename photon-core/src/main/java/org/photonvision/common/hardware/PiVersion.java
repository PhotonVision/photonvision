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

import java.io.IOException;
import org.photonvision.common.util.ShellExec;

public enum PiVersion {
    PI_B("Pi Model B"),
    COMPUTE_MODULE("Compute Module Rev"),
    ZERO_W("Pi Zero W Rev 1.1"),
    ZERO_2_W("Raspberry Pi Zero 2"),
    PI_3("Pi 3"),
    PI_4("Pi 4"),
    COMPUTE_MODULE_3("Compute Module 3"),
    UNKNOWN("UNKNOWN");

    private final String identifier;
    private static final ShellExec shell = new ShellExec(true, false);
    private static final PiVersion currentPiVersion = calcPiVersion();

    private PiVersion(String s) {
        this.identifier = s.toLowerCase();
    }

    public static PiVersion getPiVersion() {
        return currentPiVersion;
    }

    private static PiVersion calcPiVersion() {
        if (!Platform.isRaspberryPi()) return PiVersion.UNKNOWN;
        String piString = getPiVersionString();
        for (PiVersion p : PiVersion.values()) {
            if (piString.toLowerCase().contains(p.identifier)) return p;
        }
        return UNKNOWN;
    }

    // Query /proc/device-tree/model. This should return the model of the pi
    // Versions here:
    // https://github.com/raspberrypi/linux/blob/rpi-5.10.y/arch/arm/boot/dts/bcm2710-rpi-cm3.dts
    private static String getPiVersionString() {
        if (!Platform.isRaspberryPi()) return "";
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
}

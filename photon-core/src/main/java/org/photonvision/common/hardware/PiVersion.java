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

public enum PiVersion {
    PI_B("Pi Model B"),
    COMPUTE_MODULE("Compute Module Rev"),
    ZERO_W("Pi Zero W Rev 1.1"),
    PI_3("Pi 3"),
    PI_4("Pi 4"),
    COMPUTE_MODULE_3("Compute Module 3"),
    UNKNOWN("UNKNOWN");

    private final String identifier;

    PiVersion(String s) {
        this.identifier = s.toLowerCase();
    }

    public static PiVersion getPiVersion() {
        if (!Platform.isRaspberryPi()) return PiVersion.UNKNOWN;
        String piString = Platform.currentPiVersionStr;
        for (PiVersion p : PiVersion.values()) {
            if (piString.toLowerCase().contains(p.identifier)) return p;
        }
        return UNKNOWN;
    }
}

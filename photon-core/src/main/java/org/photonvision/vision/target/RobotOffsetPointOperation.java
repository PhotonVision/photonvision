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
package org.photonvision.vision.target;

public enum RobotOffsetPointOperation {
    ROPO_CLEAR(0),
    ROPO_TAKESINGLE(1),
    ROPO_TAKEFIRSTDUAL(2),
    ROPO_TAKESECONDDUAL(3);

    public final int index;

    RobotOffsetPointOperation(int index) {
        this.index = index;
    }

    public static RobotOffsetPointOperation fromIndex(int index) {
        switch (index) {
            case 0:
                return ROPO_CLEAR;
            case 1:
                return ROPO_TAKESINGLE;
            case 2:
                return ROPO_TAKEFIRSTDUAL;
            case 3:
                return ROPO_TAKESECONDDUAL;
            default:
                return ROPO_CLEAR;
        }
    }
}

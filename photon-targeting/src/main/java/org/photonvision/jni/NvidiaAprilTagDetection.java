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

package org.photonvision.jni;

public class NvidiaAprilTagDetection {
    public final int id;
    public final int hammingError;
    public final double centerX;
    public final double centerY;
    public final double[] corners;

    public NvidiaAprilTagDetection(
            int id, int hammingError, double centerX, double centerY, double[] corners) {
        this.id = id;
        this.hammingError = hammingError;
        this.centerX = centerX;
        this.centerY = centerY;
        this.corners = corners;
    }
}

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

package org.photonvision.vision.aruco;

import java.util.Arrays;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ArucoDetectionResult {
    private static final Logger logger =
            new Logger(ArucoDetectionResult.class, LogGroup.VisionModule);

    private final double[] xCorners;
    private final double[] yCorners;

    private final int id;

    public ArucoDetectionResult(double[] xCorners, double[] yCorners, int id) {
        this.xCorners = xCorners;
        this.yCorners = yCorners;
        this.id = id;
        // logger.debug("Creating a new detection result: " + this.toString());
    }

    public double[] getXCorners() {
        return xCorners;
    }

    public double[] getYCorners() {
        return yCorners;
    }

    public int getId() {
        return id;
    }

    public double getCenterX() {
        return (xCorners[0] + xCorners[1] + xCorners[2] + xCorners[3]) / 4.0;
    }

    public double getCenterY() {
        return (yCorners[0] + yCorners[1] + yCorners[2] + yCorners[3]) / 4.0;
    }

    @Override
    public String toString() {
        return "ArucoDetectionResult{"
                + "xCorners="
                + Arrays.toString(xCorners)
                + ", yCorners="
                + Arrays.toString(yCorners)
                + ", id="
                + id
                + '}';
    }
}

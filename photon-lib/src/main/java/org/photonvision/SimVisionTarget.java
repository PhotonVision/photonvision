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
package org.photonvision;

import edu.wpi.first.math.geometry.Pose2d;

public class SimVisionTarget {
    Pose2d targetPos;
    double targetWidthMeters;
    double targetHeightMeters;
    double targetHeightAboveGroundMeters;
    double tgtAreaMeters2;

    /**
    * Describes a vision target located somewhere on the field that your SimVisionSystem can detect.
    *
    * @param targetPos Pose2d of the target on the field. Define it such that, if you are standing on
    *     the middle of the field facing the target, the Y axis points to your left, and the X axis
    *     points away from you.
    * @param targetHeightAboveGroundMeters Height of the target above the field plane, in meters.
    * @param targetWidthMeters Width of the outer bounding box of the target in meters.
    * @param targetHeightMeters Pair Height of the outer bounding box of the target in meters.
    */
    public SimVisionTarget(
            Pose2d targetPos,
            double targetHeightAboveGroundMeters,
            double targetWidthMeters,
            double targetHeightMeters) {
        this.targetPos = targetPos;
        this.targetHeightAboveGroundMeters = targetHeightAboveGroundMeters;
        this.targetWidthMeters = targetWidthMeters;
        this.targetHeightMeters = targetHeightMeters;
        this.tgtAreaMeters2 = targetWidthMeters * targetHeightMeters;
    }
}

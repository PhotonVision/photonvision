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

package org.photonvision.vision.pipe.impl;

import edu.wpi.first.apriltag.AprilTagDetector;
import org.photonvision.vision.apriltag.AprilTagFamily;

public class AprilTagDetectionPipeParams {
    public final AprilTagFamily family;
    public final AprilTagDetector.Config detectorParams;

    public AprilTagDetectionPipeParams(AprilTagFamily tagFamily, AprilTagDetector.Config config) {
        this.family = tagFamily;
        this.detectorParams = config;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((family == null) ? 0 : family.hashCode());
        result = prime * result + ((detectorParams == null) ? 0 : detectorParams.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AprilTagDetectionPipeParams other = (AprilTagDetectionPipeParams) obj;
        if (family != other.family) return false;
        if (detectorParams == null) {
            if (other.detectorParams != null) return false;
        } else if (!detectorParams.equals(other.detectorParams)) return false;
        return true;
    }
}

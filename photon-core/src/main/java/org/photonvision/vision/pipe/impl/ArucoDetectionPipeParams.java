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

import java.util.Objects;
import org.opencv.aruco.Aruco;

public class ArucoDetectionPipeParams {
    /** Tag family. Default: {@link Aruco#DICT_APRILTAG_16h5}. */
    public int tagFamily = Aruco.DICT_APRILTAG_16h5;

    /** Maximum corner refinement iterations. */
    public int refinementMaxIterations = 30;

    /**
     * Minimum error (accuracy) for corner refinement in pixels. When a corner refinement iteration
     * moves the corner by less than this value, the refinement is considered finished.
     */
    public double refinementMinErrorPx = 0.1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArucoDetectionPipeParams that = (ArucoDetectionPipeParams) o;
        return tagFamily == that.tagFamily
                && refinementMaxIterations == that.refinementMaxIterations
                && refinementMinErrorPx == that.refinementMinErrorPx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagFamily, refinementMaxIterations, refinementMinErrorPx);
    }
}

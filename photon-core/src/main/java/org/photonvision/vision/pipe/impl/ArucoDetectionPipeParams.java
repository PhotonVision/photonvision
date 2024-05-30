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
import org.opencv.objdetect.Objdetect;

/** Detector parameters. See https://docs.opencv.org/4.x/d5/dae/tutorial_aruco_detection.html. */
public class ArucoDetectionPipeParams {
    /** Tag family. Default: {@link Objdetect#DICT_APRILTAG_36h11}. */
    public int tagFamily = Objdetect.DICT_APRILTAG_36h11;

    public int threshMinSize = 11;
    public int threshStepSize = 40;
    public int threshMaxSize = 91;
    public double threshConstant = 10;

    /**
     * Bits allowed to be corrected, expressed as a ratio of the tag families theoretical maximum.
     *
     * <p>E.g. 36h11 = 11 * errorCorrectionRate = Max error bits
     */
    public double errorCorrectionRate = 0;

    /**
     * If obtained corners should be iteratively refined. This should always be on for 3D estimation.
     */
    public boolean useCornerRefinement = true;

    /** Maximum corner refinement iterations. */
    public int refinementMaxIterations = 30;

    /**
     * Minimum error (accuracy) for corner refinement in pixels. When a corner refinement iteration
     * moves the corner by less than this value, the refinement is considered finished.
     */
    public double refinementMinErrorPx = 0.005;

    public boolean debugRefineWindow = false;

    /**
     * If the 'Aruco3' speedup should be used. This is similar to AprilTag's 'decimate' value, but
     * automatically determined with the given parameters.
     *
     * <p>T_i = aruco3MinMarkerSideRatio, and T_c = aruco3MinCanonicalImgSide
     *
     * <p>Scale factor = T_c / (T_c + T_i * max(img_width, img_height))
     */
    public boolean useAruco3 = false;

    /** Minimum side length of markers expressed as a ratio of the largest image dimension. */
    public double aruco3MinMarkerSideRatio = 0.02;

    /** Minimum side length of the canonical image (marker after undoing perspective distortion). */
    public int aruco3MinCanonicalImgSide = 32;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArucoDetectionPipeParams that = (ArucoDetectionPipeParams) o;
        return tagFamily == that.tagFamily
                && threshMinSize == that.threshMinSize
                && threshStepSize == that.threshStepSize
                && threshMaxSize == that.threshMaxSize
                && threshConstant == that.threshConstant
                && errorCorrectionRate == that.errorCorrectionRate
                && useCornerRefinement == that.useCornerRefinement
                && refinementMaxIterations == that.refinementMaxIterations
                && refinementMinErrorPx == that.refinementMinErrorPx
                && useAruco3 == that.useAruco3
                && aruco3MinMarkerSideRatio == that.aruco3MinMarkerSideRatio
                && aruco3MinCanonicalImgSide == that.aruco3MinCanonicalImgSide;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tagFamily,
                threshMinSize,
                threshStepSize,
                threshMaxSize,
                threshConstant,
                errorCorrectionRate,
                useCornerRefinement,
                refinementMaxIterations,
                refinementMinErrorPx,
                useAruco3,
                aruco3MinMarkerSideRatio,
                aruco3MinCanonicalImgSide);
    }
}

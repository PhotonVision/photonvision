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

import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ArucoDetectorParams {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    private ArucoDetectorParams() {}

    public static DetectorParameters getDetectorParams(
            DetectorParameters curr,
            double decimate,
            int cornerIterations,
            double minAccuracy) {
        if (curr == null
                || !(curr.get_aprilTagQuadDecimate() == decimate
                        && curr.get_cornerRefinementMaxIterations() == cornerIterations

                        && minAccuracy == curr.get_cornerRefinementMinAccuracy())) {
            DetectorParameters parameters = DetectorParameters.create();

            parameters.set_aprilTagQuadDecimate((float) decimate);
            parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_APRILTAG);
            if (cornerIterations != 0) {
                parameters.set_cornerRefinementMaxIterations(cornerIterations); // 200
            }
            if (minAccuracy != 0) {
                parameters.set_cornerRefinementMinAccuracy(
                        minAccuracy / 1000.0); // divides by 1000 because the UI multiplies it by 1000
            }
            return parameters;
        }
        return curr;
    }
}

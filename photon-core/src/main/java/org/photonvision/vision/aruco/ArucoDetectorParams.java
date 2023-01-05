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
import org.opencv.aruco.ArucoDetector;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ArucoDetectorParams {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    private float m_decimate = -1;
    private int m_iterations = -1;
    private double m_accuracy = -1;

    DetectorParameters parameters = DetectorParameters.create();
    ArucoDetector detector;

    public ArucoDetectorParams() {
        setDecimation(1);
        setCornerAccuracy(25);
        setCornerRefinementMaxIterations(100);

        detector = new ArucoDetector(Dictionary.get(Aruco.DICT_APRILTAG_16h5), parameters);
    }

    public void setDecimation(float decimate) {
        if (decimate == m_decimate) return;

        logger.info("Setting decimation from " + m_decimate + " to " + decimate);

        // We only need to mutate the parameters -- the detector keeps a poitner to the parameters
        // object internally, so it should automatically update
        parameters.set_aprilTagQuadDecimate((float) decimate);
        m_decimate = decimate;
    }

    public void setCornerRefinementMaxIterations(int iters) {
        if (iters == m_iterations || iters <= 0) return;

        parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);
        parameters.set_cornerRefinementMaxIterations(iters); // 200

        m_iterations = iters;
    }

    public void setCornerAccuracy(double accuracy) {
        if (accuracy == m_accuracy || accuracy <= 0) return;

        parameters.set_cornerRefinementMinAccuracy(
                accuracy / 1000.0); // divides by 1000 because the UI multiplies it by 1000
        m_accuracy = accuracy;
    }

    public ArucoDetector getDetector() {
        return detector;
    }
}

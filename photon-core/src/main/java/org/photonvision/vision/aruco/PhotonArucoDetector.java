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

import java.util.ArrayList;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.ArucoDetector;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * This class wraps an {@link ArucoDetector} for convenience.
 */
public class PhotonArucoDetector {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    // Detector parameters
    private final DetectorParameters params = DetectorParameters.create();
    
    private final ArucoDetector detector = new ArucoDetector(Dictionary.get(Aruco.DICT_APRILTAG_16h5), params);

    private Mat ids = new Mat();
    private ArrayList<Mat> corners = new ArrayList<Mat>();
    private Mat cornerMat;

    public PhotonArucoDetector() {
        logger.debug("New Aruco Detector");
        params.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);
    }

    public ArucoDetector getDetector() {
        return detector;
    }

    public DetectorParameters getParams() {
        return params;
    }

    /**
     * Detect fiducial tags in the grayscaled image using the {@link ArucoDetector} in this class.
     * Parameters for detection can be modified with {@link #setDetectorParams(DetectorParameters)}.
     * 
     * @param grayscaleImg A grayscaled image
     * @return An array of ArucoDetectionResult, which contain tag corners and id.
     */
    public ArucoDetectionResult[] detect(Mat grayscaleImg) {
        // detect tags
        // var param = detector.get_params();
        // logger.debug("Aruco3: "+param.get_useAruco3Detection()+", iter: "+param.get_cornerRefinementMaxIterations()+", acc: "+param.get_cornerRefinementMinAccuracy()+", method: "+param.get_cornerRefinementMethod());
        detector.detectMarkers(grayscaleImg, corners, ids);

        ArucoDetectionResult[] toReturn = new ArucoDetectionResult[corners.size()];
        for (int i = 0; i < corners.size(); i++) {
            // each tag has a cornerMat
            cornerMat = corners.get(i);
            // logger.debug(cornerMat.dump());

            // Aruco detection returns corners (TL, TR, BR, BL).
            // For parity with AprilTags and photonlib, we want (BL, BR, TR, TL).
            double[] xCorners = {
                        cornerMat.get(0, 3)[0],
                        cornerMat.get(0, 2)[0],
                        cornerMat.get(0, 1)[0],
                        cornerMat.get(0, 0)[0]
                    };
            double[] yCorners = {
                        cornerMat.get(0, 3)[1],
                        cornerMat.get(0, 2)[1],
                        cornerMat.get(0, 1)[1],
                        cornerMat.get(0, 0)[1]
                    };
            cornerMat.release();

            toReturn[i] =
                    new ArucoDetectionResult(xCorners, yCorners, (int) ids.get(i, 0)[0]);
        }
        
        ids.release();

        return toReturn;
    }
}

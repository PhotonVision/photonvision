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
import java.util.Arrays;
import org.opencv.core.Mat;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.DetectorParameters;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/** This class wraps an {@link ArucoDetector} for convenience. */
public class PhotonArucoDetector {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    private final ArucoDetector detector =
            new ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_APRILTAG_16h5));

    private Mat ids = new Mat();
    private ArrayList<Mat> cornerMats = new ArrayList<Mat>();
    private Mat cornerMat;

    public ArucoDetector getDetector() {
        return detector;
    }

    /**
     * Get a copy of the current parameters being used. Must next call setParams to update the
     * underlying detector object!
     */
    public DetectorParameters getParams() {
        return detector.getDetectorParameters();
    }

    public void setParams(DetectorParameters params) {
        detector.setDetectorParameters(params);
    }

    /**
     * Detect fiducial tags in the grayscaled image using the {@link ArucoDetector} in this class.
     * Parameters for detection can be modified with {@link #setParams(DetectorParameters)}.
     *
     * @param grayscaleImg A grayscaled image
     * @return An array of ArucoDetectionResult, which contain tag corners and id.
     */
    public ArucoDetectionResult[] detect(Mat grayscaleImg) {
        // detect tags
        detector.detectMarkers(grayscaleImg, cornerMats, ids);

        ArucoDetectionResult[] toReturn = new ArucoDetectionResult[cornerMats.size()];
        for (int i = 0; i < cornerMats.size(); i++) {
            // each detection has a Mat of corners
            cornerMat = cornerMats.get(i);

            // Aruco detection returns corners (BR, BL, TL, TR).
            // For parity with AprilTags and photonlib, we want (BL, BR, TR, TL).
            double[] xCorners = {
                cornerMat.get(0, 1)[0],
                cornerMat.get(0, 0)[0],
                cornerMat.get(0, 3)[0],
                cornerMat.get(0, 2)[0]
            };
            double[] yCorners = {
                cornerMat.get(0, 1)[1],
                cornerMat.get(0, 0)[1],
                cornerMat.get(0, 3)[1],
                cornerMat.get(0, 2)[1]
            };
            cornerMat.release();

            toReturn[i] = new ArucoDetectionResult(xCorners, yCorners, (int) ids.get(i, 0)[0]);
        }

        ids.release();

        // sort tags by ID
        Arrays.sort(
                toReturn,
                (ArucoDetectionResult a, ArucoDetectionResult b) -> {
                    if (a.getId() > b.getId()) {
                        return 1;
                    } else if (a.getId() < b.getId()) {
                        return -1;
                    } else return 0;
                });

        return toReturn;
    }
}

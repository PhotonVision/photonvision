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

import org.wpilib.vision.apriltag.AprilTagDetection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.opencv.core.Mat;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.DetectorParameters;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.Releasable;

/** This class wraps an {@link ArucoDetector} for convenience. */
public class PhotonArucoDetector implements Releasable {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    private final ArucoDetector detector =
            new ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_APRILTAG_16h5));

    private final Mat ids = new Mat();
    private final ArrayList<Mat> cornerMats = new ArrayList<>();

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
     * @return An array of AprilTagDetection, which contain tag corners and id.
     */
    public AprilTagDetection[] detect(Mat grayscaleImg) {
        // detect tags
        detector.detectMarkers(grayscaleImg, cornerMats, ids);

        AprilTagDetection[] results = new AprilTagDetection[cornerMats.size()];
        for (int i = 0; i < cornerMats.size(); i++) {
            // each detection has a Mat of corners
            Mat cornerMat = cornerMats.get(i);

            // ArUco detection returns corners (BR, BL, TL, TR).
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

            // AprilTagDetection corners: [x0,y0,x1,y1,x2,y2,x3,y3] in order BL,BR,TR,TL
            double[] corners = {
                xCorners[0], yCorners[0],
                xCorners[1], yCorners[1],
                xCorners[2], yCorners[2],
                xCorners[3], yCorners[3]
            };
            double centerX = (xCorners[0] + xCorners[1] + xCorners[2] + xCorners[3]) / 4.0;
            double centerY = (yCorners[0] + yCorners[1] + yCorners[2] + yCorners[3]) / 4.0;
            int id = (int) ids.get(i, 0)[0];

            // OpenCV's ArucoDetector does not provide hamming, decision margin, or homography.
            results[i] = new AprilTagDetection("", id, 0, 0f, null, centerX, centerY, corners);
        }

        ids.release();
        for (var m : cornerMats) m.release();
        cornerMats.clear();

        // sort tags by ID
        Arrays.sort(results, Comparator.comparingInt(AprilTagDetection::getId));

        return results;
    }

    @Override
    public void release() {
        ids.release();
        for (var m : cornerMats) m.release();
        cornerMats.clear();
    }
}

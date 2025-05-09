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

package org.photonvision.jni;

import org.opencv.core.Mat;

public final class ArucoNanoV5Detector {
    public static record DetectionResult(double[] xCorners, double[] yCorners, int id) {}

    /**
     * Detects Aruco markers.
     *
     * @param matPtr The pointer to the Mat to detect tags from.
     * @return A double array with the detections. Each detection is encoded as 9 doubles,
     *     representing the XY corner coordinates for the 4 corners and a tag ID at the end.
     */
    private static native double[] detect(long matPtr);

    public static DetectionResult[] detect(Mat mat) {
        var detectionData = detect(mat.getNativeObjAddr());
        DetectionResult[] detections = new DetectionResult[(int) (detectionData.length / 9)];
        for (int i = 0; i < detectionData.length; i += 9) {
            double[] xCorners = {
                detectionData[i + 0], detectionData[i + 2], detectionData[i + 4], detectionData[i + 6]
            };
            double[] yCorners = {
                detectionData[i + 1], detectionData[i + 3], detectionData[i + 5], detectionData[i + 7]
            };
            detections[(int) (i / 9)] =
                    new DetectionResult(xCorners, yCorners, (int) detectionData[i + 8]);
        }
        return detections;
    }
}

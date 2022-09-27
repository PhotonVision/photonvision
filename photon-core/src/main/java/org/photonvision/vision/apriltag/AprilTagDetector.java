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

package org.photonvision.vision.apriltag;

import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

public class AprilTagDetector {
    private static final Logger logger = new Logger(AprilTagDetector.class, LogGroup.VisionModule);
    private long m_detectorPtr = 0;
    private AprilTagDetectorParams m_detectorParams = AprilTagDetectorParams.DEFAULT_36H11;

    public AprilTagDetector() {
        updateDetector();
    }

    private void updateDetector() {
        if (m_detectorPtr != 0) {
            // TODO: in JNI
            AprilTagJNI.AprilTag_Destroy(m_detectorPtr);
            m_detectorPtr = 0;
        }

        logger.debug("Creating detector with params " + m_detectorParams);
        m_detectorPtr =
                AprilTagJNI.AprilTag_Create(
                        m_detectorParams.tagFamily.getNativeName(),
                        m_detectorParams.decimate,
                        m_detectorParams.blur,
                        m_detectorParams.threads,
                        m_detectorParams.debug,
                        m_detectorParams.refineEdges);
    }

    public void updateParams(AprilTagDetectorParams newParams) {
        if (!m_detectorParams.equals(newParams)) {
            m_detectorParams = newParams;
            updateDetector();
        }
    }

    public DetectionResult[] detect(
            Mat grayscaleImg,
            CameraCalibrationCoefficients coeffs,
            boolean useNativePoseEst,
            int numIterations,
            double tagWidthMeters) {
        if (m_detectorPtr == 0) {
            // Detector not set up (JNI issue? or similar?)
            // No detection is possible.
            return new DetectionResult[] {};
        }

        var cx = 0.0;
        var cy = 0.0;
        var fx = 0.0;
        var fy = 0.0;
        var doPoseEst = false;

        if (coeffs != null && useNativePoseEst) {
            final Mat cameraMatrix = coeffs.getCameraIntrinsicsMat();
            if (cameraMatrix != null) {
                // Camera calibration has been done, we should be able to do pose estimation
                cx = cameraMatrix.get(0, 2)[0];
                cy = cameraMatrix.get(1, 2)[0];
                fx = cameraMatrix.get(0, 0)[0];
                fy = cameraMatrix.get(1, 1)[0];
                doPoseEst = true;
            }
        }

        return AprilTagJNI.AprilTag_Detect(
                m_detectorPtr, grayscaleImg, doPoseEst, tagWidthMeters, fx, fy, cx, cy, numIterations);
    }
}

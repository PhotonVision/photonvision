/*
Copyright (c) 2022 Photon Vision. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of FIRST, WPILib, nor the names of other WPILib
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY FIRST AND OTHER WPILIB CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FIRST OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        //updateDetector();
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

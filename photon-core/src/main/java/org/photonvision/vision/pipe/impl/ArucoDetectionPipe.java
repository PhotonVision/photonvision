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

import org.opencv.core.Mat;
import org.photonvision.vision.apriltag.AprilTagDetector;
import org.photonvision.vision.apriltag.DetectionResult;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.aruco.ArucoDetector;
import org.photonvision.vision.pipe.CVPipe;

import java.util.List;

public class ArucoDetectionPipe
        extends CVPipe<Mat, List<ArucoDetectionResult>, ArucoDetectionPipeParams> {

    ArucoDetector detector = new ArucoDetector();
    @Override
    protected List<ArucoDetectionResult> process(Mat in) {
        return List.of(detector.detect(in, params.cameraCalibrationCoefficients));
    }

    @Override
    public void setParams(ArucoDetectionPipeParams params) {
        super.setParams(params);
       // m_detector.updateParams(params.detectorParams);
    }
}

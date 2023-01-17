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

import edu.wpi.first.math.util.Units;
import java.util.List;
import org.opencv.aruco.DetectorParameters;
import org.opencv.core.Mat;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.aruco.PhotonArucoDetector;
import org.photonvision.vision.pipe.CVPipe;

public class ArucoDetectionPipe
        extends CVPipe<Mat, List<ArucoDetectionResult>, ArucoDetectionPipeParams> {
    PhotonArucoDetector detector = new PhotonArucoDetector();

    @Override
    protected List<ArucoDetectionResult> process(Mat in) {
        return List.of(
                detector.detect(
                        in,
                        (float) Units.inchesToMeters(6),
                        params.cameraCalibrationCoefficients,
                        params.detectorParams));
    }

    @Override
    public void setParams(ArucoDetectionPipeParams params) {
        super.setParams(params);
    }

    public DetectorParameters getParameters() {
        return params == null ? null : params.detectorParams.get_params();
    }
}

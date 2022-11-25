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

import org.photonvision.vision.apriltag.AprilTagDetectorParams;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.aruco.ArucoDetectorParams;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

import java.util.Objects;

public class ArucoDetectionPipeParams {
    public final ArucoDetectorParams detectorParams;
    public final CameraCalibrationCoefficients cameraCalibrationCoefficients;


    public ArucoDetectionPipeParams(
            double decimate, int cornerIterations, boolean useAruco3,
            CameraCalibrationCoefficients cameraCalibrationCoefficients) {
        detectorParams = new ArucoDetectorParams(decimate, cornerIterations, useAruco3);
        this.cameraCalibrationCoefficients = cameraCalibrationCoefficients;

    }

    public ArucoDetectionPipeParams(
            ArucoDetectorParams detectorParams,
            CameraCalibrationCoefficients cameraCalibrationCoefficients
            ) {
        this.detectorParams = detectorParams;
        this.cameraCalibrationCoefficients = cameraCalibrationCoefficients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArucoDetectionPipeParams that = (ArucoDetectionPipeParams) o;
        return Objects.equals(detectorParams, that.detectorParams)
                && Objects.equals(cameraCalibrationCoefficients, that.cameraCalibrationCoefficients);
    }

    @Override
    public String toString() {
        return "AprilTagDetectionPipeParams{"
                + "detectorParams="
                + detectorParams
                + ", cameraCalibrationCoefficients="
                + cameraCalibrationCoefficients
                + '}';
    }
}

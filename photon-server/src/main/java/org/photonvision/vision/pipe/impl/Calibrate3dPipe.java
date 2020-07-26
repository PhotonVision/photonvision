/*
 * Copyright (C) 2020 Photon Vision.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.JsonMat;
import org.photonvision.vision.pipe.CVPipe;

public class Calibrate3dPipe
        extends CVPipe<
                List<List<Mat>>, CameraCalibrationCoefficients, Calibrate3dPipe.CalibratePipeParams> {

    // Camera matrix stores the center of the image and focal length across the x and y-axis in a 3x3
    // matrix
    private Mat cameraMatrix = new Mat();
    // Stores the radical and tangential distortion in a 5x1 matrix
    private MatOfDouble distortionCoefficients = new MatOfDouble();

    // For loggging
    private static final Logger logger = new Logger(Calibrate3dPipe.class, LogGroup.General);

    // Translational and rotational matrices
    private List<Mat> rvecs = new ArrayList<>();
    private List<Mat> tvecs = new ArrayList<>();

    // The Standard deviation of the estimated parameters
    private Mat stdDeviationsIntrinsics = new Mat();
    private Mat stdDeviationsExtrinsics = new Mat();

    // Contains the re projection error of each snapshot by re projecting the corners we found and
    // finding the euclidean distance between the actual corners.
    private Mat perViewErrors = new Mat();

    // RMS of the calibration
    private double calibrationAccuracy;

    /**
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return Result of processing.
    */
    @Override
    protected CameraCalibrationCoefficients process(List<List<Mat>> in) {
        try {
            // FindBoardCorners pipe outputs all the image points, object points, and frames to calculate
            // imageSize from, other parameters are output Mats
            calibrationAccuracy =
                    Calib3d.calibrateCameraExtended(
                            in.get(1),
                            in.get(2),
                            new Size(in.get(0).get(0).width(), in.get(0).get(0).height()),
                            cameraMatrix,
                            distortionCoefficients,
                            rvecs,
                            tvecs,
                            stdDeviationsIntrinsics,
                            stdDeviationsExtrinsics,
                            perViewErrors);
        } catch (Exception e) {
            logger.error("Calibration failed!", e);
        }
        JsonMat cameraMatrixMat = JsonMat.fromMat(cameraMatrix);
        JsonMat distortionCoefficientsMat = JsonMat.fromMat(distortionCoefficients);
        try {
            // Print calibration successful
            logger.info(
                    "CALIBRATION SUCCESS (with accuracy "
                            + calibrationAccuracy
                            + ")! camMatrix: \n"
                            + new ObjectMapper().writeValueAsString(cameraMatrixMat)
                            + "\ndistortionCoeffs:\n"
                            + new ObjectMapper().writeValueAsString(distortionCoefficientsMat)
                            + "\n");
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse calibration data to json!", e);
        }
        // Create a new CameraCalibrationCoefficients object to pass onto SolvePnP
        double[] perViewErrorsArray =
                new double[(int) perViewErrors.total() * perViewErrors.channels()];
        perViewErrors.get(0, 0, perViewErrorsArray);
        return new CameraCalibrationCoefficients(
                params.resolution, cameraMatrixMat, distortionCoefficientsMat, perViewErrorsArray);
    }

    public static class CalibratePipeParams {
        // Only needs resolution to pass onto CameraCalibrationCoefficients object.
        private final Size resolution;

        public CalibratePipeParams(Size resolution) {
            this.resolution = resolution;
        }
    }
}

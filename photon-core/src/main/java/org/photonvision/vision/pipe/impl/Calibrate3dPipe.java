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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.BoardObservation;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.JsonMat;
import org.photonvision.vision.pipe.CVPipe;

public class Calibrate3dPipe
        extends CVPipe<
                List<Triple<Size, Mat, Mat>>,
                CameraCalibrationCoefficients,
                Calibrate3dPipe.CalibratePipeParams> {
    // Camera matrix stores the center of the image and focal length across the x and y-axis in a 3x3
    // matrix
    private final Mat cameraMatrix = new Mat();
    // Stores the radical and tangential distortion in a 5x1 matrix
    private final MatOfDouble distortionCoefficients = new MatOfDouble();

    // For logging
    private static final Logger logger = new Logger(Calibrate3dPipe.class, LogGroup.General);

    // Translational and rotational matrices
    private final List<Mat> rvecs = new ArrayList<>();
    private final List<Mat> tvecs = new ArrayList<>();

    // The Standard deviation of the estimated parameters
    private final Mat stdDeviationsIntrinsics = new Mat();
    private final Mat stdDeviationsExtrinsics = new Mat();

    // Contains the re projection error of each snapshot by re projecting the corners we found and
    // finding the Euclidean distance between the actual corners.
    private final Mat perViewErrors = new Mat();

    // RMS of the calibration
    private double calibrationAccuracy;

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing. In the format (Input image, object points, image points)
     * @return Result of processing.
     */
    @Override
    protected CameraCalibrationCoefficients process(List<Triple<Size, Mat, Mat>> in) {
        in =
                in.stream()
                        .filter(
                                it ->
                                        it != null
                                                && it.getLeft() != null
                                                && it.getMiddle() != null
                                                && it.getRight() != null)
                        .collect(Collectors.toList());

        List<Mat> objPoints = in.stream().map(Triple::getMiddle).collect(Collectors.toList());
        List<Mat> imgPts = in.stream().map(Triple::getRight).collect(Collectors.toList());
        if (objPoints.size() != imgPts.size()) {
            logger.error("objpts.size != imgpts.size");
            return null;
        }

        try {
            // FindBoardCorners pipe outputs all the image points, object points, and frames to calculate
            // imageSize from, other parameters are output Mats

            calibrationAccuracy =
                    Calib3d.calibrateCameraExtended(
                            objPoints,
                            imgPts,
                            new Size(in.get(0).getLeft().width, in.get(0).getLeft().height),
                            cameraMatrix,
                            distortionCoefficients,
                            rvecs,
                            tvecs,
                            stdDeviationsIntrinsics,
                            stdDeviationsExtrinsics,
                            perViewErrors);
        } catch (Exception e) {
            logger.error("Calibration failed!", e);
            e.printStackTrace();
            return null;
        }

        JsonMat cameraMatrixMat = JsonMat.fromMat(cameraMatrix);
        JsonMat distortionCoefficientsMat = JsonMat.fromMat(distortionCoefficients);

        // For each observation, calc reprojection error
        Mat jac_temp = new Mat();
        List<BoardObservation> observations = new ArrayList<>();
        for (int i = 0; i < objPoints.size(); i++) {
            MatOfPoint3f i_objPtsNative = new MatOfPoint3f();
            objPoints.get(i).copyTo(i_objPtsNative);
            var i_objPts = i_objPtsNative.toList();
            var i_imgPts = ((MatOfPoint2f) imgPts.get(i)).toList();

            var img_pts_reprojected = new MatOfPoint2f();
            try {
                Calib3d.projectPoints(
                        i_objPtsNative,
                        rvecs.get(i),
                        tvecs.get(i),
                        cameraMatrix,
                        distortionCoefficients,
                        img_pts_reprojected,
                        jac_temp,
                        0.0);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            var img_pts_reprojected_list = img_pts_reprojected.toList();

            var reprojectionError = new ArrayList<Point>();
            for (int j = 0; j < img_pts_reprojected_list.size(); j++) {
                // error = (measured - expected)
                var measured = img_pts_reprojected_list.get(j);
                var expected = i_imgPts.get(j);
                var error = new Point(measured.x - expected.x, measured.y - expected.y);
                reprojectionError.add(error);
            }

            var camToBoard = MathUtils.opencvRTtoPose3d(rvecs.get(i), tvecs.get(i));

            observations.add(new BoardObservation(i_objPts, i_imgPts, reprojectionError, camToBoard, true));
        }
        jac_temp.release();

        // Standard deviation of results
        try {
            // Print calibration successful
            logger.info(
                    "CALIBRATION SUCCESS for res "
                            + params.resolution
                            + " (with accuracy "
                            + calibrationAccuracy
                            + ")! camMatrix: \n"
                            + new ObjectMapper().writeValueAsString(cameraMatrixMat)
                            + "\ndistortionCoeffs:\n"
                            + new ObjectMapper().writeValueAsString(distortionCoefficientsMat)
                            + "\n");
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse calibration data to json!", e);
        }
        return new CameraCalibrationCoefficients(
                params.resolution, cameraMatrixMat, distortionCoefficientsMat, observations);
    }

    public static class CalibratePipeParams {
        // Only needs resolution to pass onto CameraCalibrationCoefficients object.
        private final Size resolution;

        public CalibratePipeParams(Size resolution) {
            //            logger.info("res: " + resolution.toString());
            this.resolution = resolution;
        }
    }
}

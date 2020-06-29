package org.photonvision.vision.pipe.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
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
            e.printStackTrace();
        }
        JsonMat cameraMatrixMat = JsonMat.fromMat(cameraMatrix);
        JsonMat distortionCoefficientsMat = JsonMat.fromMat(distortionCoefficients);
        try {
            // Print calibration successful
            System.out.printf(
                    "CALIBRATION SUCCESS (with accuracy %s)! camMatrix: \n%s\ndistortionCoeffs:\n%s\n",
                    calibrationAccuracy,
                    new ObjectMapper().writeValueAsString(cameraMatrixMat),
                    new ObjectMapper().writeValueAsString(distortionCoefficientsMat));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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

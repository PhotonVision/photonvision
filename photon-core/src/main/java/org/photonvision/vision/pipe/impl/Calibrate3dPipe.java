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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.mrcal.MrCalJNI;
import org.photonvision.mrcal.MrCalJNI.MrCalResult;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.calibration.BoardObservation;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonImageMat;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.pipe.CVPipe;

public class Calibrate3dPipe
        extends CVPipe<
                List<FindBoardCornersPipe.FindBoardCornersPipeResult>,
                CameraCalibrationCoefficients,
                Calibrate3dPipe.CalibratePipeParams> {
    // For logging
    private static final Logger logger = new Logger(Calibrate3dPipe.class, LogGroup.General);

    // The Standard deviation of the estimated parameters
    private final Mat stdDeviationsIntrinsics = new Mat();
    private final Mat stdDeviationsExtrinsics = new Mat();

    // Contains the re projection error of each snapshot by re projecting the corners we found and
    // finding the Euclidean distance between the actual corners.
    private final Mat perViewErrors = new Mat();

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing. In the format (Input image, object points, image points)
     * @return Result of processing.
     */
    @Override
    protected CameraCalibrationCoefficients process(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in) {
        in =
                in.stream()
                        .filter(
                                it ->
                                        it != null
                                                && it.imagePoints != null
                                                && it.objectPoints != null
                                                && it.size != null)
                        .collect(Collectors.toList());

        CameraCalibrationCoefficients ret;
        var start = System.nanoTime();
        if (MrCalJNILoader.isWorking() && params.useMrCal) {
            logger.debug("Calibrating with mrcal!");
            ret = calibrateMrcal(in);
        } else {
            logger.debug("Calibrating with opencv!");
            ret = calibrateOpenCV(in);
        }
        var dt = System.nanoTime() - start;

        if (ret != null)
            logger.info(
                    "CALIBRATION SUCCESS for res "
                            + in.get(0).size
                            + " in "
                            + dt / 1e6
                            + "ms! camMatrix: \n"
                            + Arrays.toString(ret.cameraIntrinsics.data)
                            + "\ndistortionCoeffs:\n"
                            + Arrays.toString(ret.distCoeffs.data)
                            + "\n");
        else logger.info("Calibration failed! Review log for more details");

        return ret;
    }

    protected CameraCalibrationCoefficients calibrateOpenCV(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in) {
        List<Mat> objPoints = in.stream().map(it -> it.objectPoints).collect(Collectors.toList());
        List<Mat> imgPts = in.stream().map(it -> it.imagePoints).collect(Collectors.toList());
        if (objPoints.size() != imgPts.size()) {
            logger.error("objpts.size != imgpts.size");
            return null;
        }

        Mat cameraMatrix = new Mat();
        MatOfDouble distortionCoefficients = new MatOfDouble();
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        // RMS of the calibration
        double calibrationAccuracy;

        try {
            // FindBoardCorners pipe outputs all the image points, object points, and frames to calculate
            // imageSize from, other parameters are output Mats

            calibrationAccuracy =
                    Calib3d.calibrateCameraExtended(
                            objPoints,
                            imgPts,
                            new Size(in.get(0).size.width, in.get(0).size.height),
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

        JsonMatOfDouble cameraMatrixMat = JsonMatOfDouble.fromMat(cameraMatrix);
        JsonMatOfDouble distortionCoefficientsMat = JsonMatOfDouble.fromMat(distortionCoefficients);

        var observations =
                createObservations(in, cameraMatrix, distortionCoefficients, rvecs, tvecs, null);

        cameraMatrix.release();
        distortionCoefficients.release();
        rvecs.forEach(Mat::release);
        tvecs.forEach(Mat::release);

        return new CameraCalibrationCoefficients(
                in.get(0).size,
                cameraMatrixMat,
                distortionCoefficientsMat,
                new double[0],
                observations,
                new Size(params.boardWidth, params.boardHeight),
                params.squareSize,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    protected CameraCalibrationCoefficients calibrateMrcal(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in) {
        List<MatOfPoint2f> corner_locations =
                in.stream().map(it -> it.imagePoints).map(MatOfPoint2f::new).collect(Collectors.toList());

        int imageWidth = (int) in.get(0).size.width;
        int imageHeight = (int) in.get(0).size.height;
        final double FOCAL_LENGTH_GUESS = 1200;

        MrCalResult result =
                MrCalJNI.calibrateCamera(
                        corner_locations,
                        params.boardWidth,
                        params.boardHeight,
                        params.squareSize,
                        imageWidth,
                        imageHeight,
                        FOCAL_LENGTH_GUESS);

        // intrinsics are fx fy cx cy from mrcal
        JsonMatOfDouble cameraMatrixMat =
                new JsonMatOfDouble(
                        3,
                        3,
                        CvType.CV_64FC1,
                        new double[] {
                            // fx 0 cx
                            result.intrinsics[0],
                            0,
                            result.intrinsics[2],
                            // 0 fy cy
                            0,
                            result.intrinsics[1],
                            result.intrinsics[3],
                            // 0 0 1
                            0,
                            0,
                            1
                        });
        JsonMatOfDouble distortionCoefficientsMat =
                new JsonMatOfDouble(1, 8, CvType.CV_64FC1, Arrays.copyOfRange(result.intrinsics, 4, 12));

        // Calculate optimized board poses manually. We get this for free from mrcal too, but that's not
        // JNIed (yet)
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        for (var o : in) {
            var rvec = new Mat();
            var tvec = new Mat();
            Calib3d.solvePnP(
                    o.objectPoints,
                    o.imagePoints,
                    cameraMatrixMat.getAsMat(),
                    distortionCoefficientsMat.getAsMatOfDouble(),
                    rvec,
                    tvec);
            rvecs.add(rvec);
            tvecs.add(tvec);
        }

        List<BoardObservation> observations =
                createObservations(
                        in,
                        cameraMatrixMat.getAsMat(),
                        distortionCoefficientsMat.getAsMatOfDouble(),
                        rvecs,
                        tvecs,
                        new double[] {result.warp_x, result.warp_y});

        rvecs.forEach(Mat::release);
        tvecs.forEach(Mat::release);

        return new CameraCalibrationCoefficients(
                in.get(0).size,
                cameraMatrixMat,
                distortionCoefficientsMat,
                new double[] {result.warp_x, result.warp_y},
                observations,
                new Size(params.boardWidth, params.boardHeight),
                params.squareSize,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    private List<BoardObservation> createObservations(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in,
            Mat cameraMatrix_,
            MatOfDouble distortionCoefficients_,
            List<Mat> rvecs,
            List<Mat> tvecs,
            double[] calobject_warp) {
        List<Mat> objPoints = in.stream().map(it -> it.objectPoints).collect(Collectors.toList());
        List<Mat> imgPts = in.stream().map(it -> it.imagePoints).collect(Collectors.toList());

        // For each observation, calc reprojection error
        Mat jac_temp = new Mat();
        List<BoardObservation> observations = new ArrayList<>();
        for (int i = 0; i < objPoints.size(); i++) {
            MatOfPoint3f i_objPtsNative = new MatOfPoint3f();
            objPoints.get(i).copyTo(i_objPtsNative);
            var i_objPts = i_objPtsNative.toList();
            var i_imgPts = ((MatOfPoint2f) imgPts.get(i)).toList();

            // Apply warp, if set
            if (calobject_warp != null && calobject_warp.length == 2) {
                // mrcal warp model!
                // The chessboard spans [-1, 1] on the x and y axies. We then let z=k_x(1-x^2)+k_y(1-y^2)

                double xmin = 0;
                double ymin = 0;
                double xmax = params.boardWidth * params.squareSize;
                double ymax = params.boardHeight * params.squareSize;
                double k_x = calobject_warp[0];
                double k_y = calobject_warp[1];

                // Convert to list, remap z, and back to cv::Mat
                var list = i_objPtsNative.toArray();
                for (var pt : list) {
                    double x_norm = MathUtils.map(pt.x, xmin, xmax, -1, 1);
                    double y_norm = MathUtils.map(pt.y, ymin, ymax, -1, 1);
                    pt.z = k_x * (1 - x_norm * x_norm) + k_y * (1 - y_norm * y_norm);
                }
                i_objPtsNative.fromArray(list);
            }

            var img_pts_reprojected = new MatOfPoint2f();
            try {
                Calib3d.projectPoints(
                        i_objPtsNative,
                        rvecs.get(i),
                        tvecs.get(i),
                        cameraMatrix_,
                        distortionCoefficients_,
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

            JsonImageMat image = null;
            var inputImage = in.get(i).inputImage;
            if (inputImage != null) {
                image = new JsonImageMat(inputImage);
            }
            observations.add(
                    new BoardObservation(
                            i_objPts, i_imgPts, reprojectionError, camToBoard, true, "img" + i + ".png", image));
        }
        jac_temp.release();

        return observations;
    }

    public static class CalibratePipeParams {
        // Size (in # of corners) of the calibration object
        public int boardHeight;
        public int boardWidth;
        // And size of each square
        public double squareSize;

        public boolean useMrCal;

        public CalibratePipeParams(
                int boardHeightSquares, int boardWidthSquares, double squareSize, boolean usemrcal) {
            this.boardHeight = boardHeightSquares - 1;
            this.boardWidth = boardWidthSquares - 1;
            this.squareSize = squareSize;
            this.useMrCal = usemrcal;
        }
    }
}

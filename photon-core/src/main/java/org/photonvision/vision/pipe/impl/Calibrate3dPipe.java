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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.mrcal.MrCalJNI;
import org.photonvision.mrcal.MrCalJNI.MrCalObservation;
import org.photonvision.mrcal.MrCalJNI.MrCalResult;
import org.photonvision.vision.calibration.BoardObservation;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipe;

public class Calibrate3dPipe
        extends CVPipe<
                Calibrate3dPipe.CalibrationInput,
                CameraCalibrationCoefficients,
                Calibrate3dPipe.CalibratePipeParams> {
    public static class CalibrationInput {
        final List<FindBoardCornersPipe.FindBoardCornersPipeResult> observations;
        final FrameStaticProperties imageProps;
        final Path imageSavePath;

        public CalibrationInput(
                List<FindBoardCornersPipe.FindBoardCornersPipeResult> observations,
                FrameStaticProperties imageProps,
                Path imageSavePath) {
            this.observations =
                    observations.stream()
                            .filter(
                                    observation ->
                                            observation != null
                                                    && observation.imagePoints != null
                                                    && observation.objectPoints != null
                                                    && observation.size != null)
                            .toList();
            this.imageProps = imageProps;
            this.imageSavePath = imageSavePath;
        }
    }

    // For logging
    private static final Logger logger = new Logger(Calibrate3dPipe.class, LogGroup.General);

    // The Standard deviation of the estimated parameters
    private final Mat stdDeviationsIntrinsics = new Mat();
    private final Mat stdDeviationsExtrinsics = new Mat();

    // Contains the re projection error of each snapshot by re projecting the
    // corners we found and
    // finding the Euclidean distance between the actual corners.
    private final Mat perViewErrors = new Mat();

    final MatOfPoint3f objectPoints = new MatOfPoint3f();
    private final MatOfPoint2f projectedMat = new MatOfPoint2f();

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing. In the format (Input image, object points, image points)
     * @return Result of processing.
     */
    @Override
    protected CameraCalibrationCoefficients process(CalibrationInput in) {
        CameraCalibrationCoefficients ret;
        var start = System.nanoTime();

        if (LoadJNI.hasLoaded(JNITypes.MRCAL) && params.useMrCal) {
            logger.debug("Calibrating with mrcal!");
            ret = calibrateMrcal(in.observations, in.imageProps, in.imageSavePath);
        } else {
            logger.debug("Calibrating with opencv!");
            ret = calibrateOpenCV(in.observations, in.imageProps, in.imageSavePath);
        }

        var dt = System.nanoTime() - start;

        if (ret != null)
            logger.info(
                    "CALIBRATION SUCCESS for res "
                            + in.observations.get(0).size
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
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> observationCorners,
            FrameStaticProperties imageProps,
            Path imageSavePath) {
        // The observation levels are ignored since they are never used to skip points with the
        // current detectors. If this changes, the relevant points should be filtered out so they are
        // not processed.

        Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
        MatOfDouble distortionCoefficients = new MatOfDouble();
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        // initial camera matrix guess
        double cx = (observationCorners.get(0).size.width / 2.0) - 0.5;
        double cy = (observationCorners.get(0).size.height / 2.0) - 0.5;
        cameraMatrix.put(
                0,
                0,
                new double[] {
                    imageProps.horizontalFocalLength, 0, cx, 0, imageProps.verticalFocalLength, cy, 0, 0, 1
                });

        try {
            // FindBoardCorners pipe outputs all the image points, object points, and frames to
            // calculate imageSize from, other parameters are output Mats

            Calib3d.calibrateCameraExtended(
                    observationCorners.stream().map(it -> (Mat) it.objectPoints).toList(),
                    observationCorners.stream().map(it -> (Mat) it.imagePoints).toList(),
                    new Size(observationCorners.get(0).size.width, observationCorners.get(0).size.height),
                    cameraMatrix,
                    distortionCoefficients,
                    rvecs,
                    tvecs,
                    stdDeviationsIntrinsics,
                    stdDeviationsExtrinsics,
                    perViewErrors,
                    Calib3d.CALIB_USE_LU + Calib3d.CALIB_USE_INTRINSIC_GUESS);
        } catch (Exception e) {
            logger.error("Calibration failed!", e);
            e.printStackTrace();
            cameraMatrix.release();
            distortionCoefficients.release();
            return null;
        }

        JsonMatOfDouble cameraMatrixMat = JsonMatOfDouble.fromMat(cameraMatrix);
        JsonMatOfDouble distortionCoefficientsMat = JsonMatOfDouble.fromMat(distortionCoefficients);

        // Opencv is lame, so we can only assume all points are inliers
        var inliners =
                observationCorners.stream()
                        .map(
                                it -> {
                                    var array = new boolean[it.objectPoints.rows() * it.objectPoints.cols()];
                                    Arrays.fill(array, true);
                                    return array;
                                })
                        .toList();

        var observations =
                createObservations(
                        observationCorners,
                        cameraMatrix,
                        distortionCoefficients,
                        rvecs,
                        tvecs,
                        inliners,
                        new double[] {0, 0},
                        imageSavePath);

        cameraMatrix.release();
        distortionCoefficients.release();
        rvecs.forEach(Mat::release);
        tvecs.forEach(Mat::release);

        return new CameraCalibrationCoefficients(
                observationCorners.get(0).size,
                cameraMatrixMat,
                distortionCoefficientsMat,
                new double[0],
                observations,
                new Size(params.boardWidth, params.boardHeight),
                params.squareSize,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    protected CameraCalibrationCoefficients calibrateMrcal(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> observationCorners,
            FrameStaticProperties imageProps,
            Path imageSavePath) {
        Iterator<MrCalObservation> observationData =
                observationCorners.stream()
                        .map(
                                it -> {
                                    var corners = it.imagePoints.toArray();

                                    var levels = new float[(int) it.imagePoints.total()];
                                    Arrays.fill(levels, it.level);

                                    var ids = it.ids != null ? it.ids.toArray() : null;

                                    return new MrCalObservation(corners, levels, ids);
                                })
                        .iterator();

        int imageWidth = (int) observationCorners.get(0).size.width;
        int imageHeight = (int) observationCorners.get(0).size.height;

        MrCalResult result =
                MrCalJNI.calibrateCamera(
                        observationCorners.size(),
                        observationData,
                        params.boardWidth,
                        params.boardHeight,
                        params.squareSize,
                        imageWidth,
                        imageHeight,
                        (imageProps.horizontalFocalLength + imageProps.verticalFocalLength) / 2.0);

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

        // We get these from the JNI (retsult.optimizedPoses), but these are subtly different from the
        // ones our code used to produce. To preserve consistency, continue to redo this math
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        for (var o : observationCorners) {
            var rvec = new Mat();
            var tvec = new Mat();

            Calib3d.solvePnP(
                    o.objectPoints,
                    o.imagePoints,
                    cameraMatrixMat.getAsMatOfDouble(),
                    distortionCoefficientsMat.getAsMatOfDouble(),
                    rvec,
                    tvec);
            rvecs.add(rvec);
            tvecs.add(tvec);
        }

        List<BoardObservation> observations =
                createObservations(
                        observationCorners,
                        cameraMatrixMat.getAsMatOfDouble(),
                        distortionCoefficientsMat.getAsMatOfDouble(),
                        rvecs,
                        tvecs,
                        result.cornersUsed,
                        new double[] {result.warp_x, result.warp_y},
                        imageSavePath);

        rvecs.forEach(Mat::release);
        tvecs.forEach(Mat::release);

        return new CameraCalibrationCoefficients(
                observationCorners.get(0).size,
                cameraMatrixMat,
                distortionCoefficientsMat,
                new double[] {result.warp_x, result.warp_y},
                observations,
                new Size(params.boardWidth, params.boardHeight),
                params.squareSize,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    private List<BoardObservation> createObservations(
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> observationData,
            Mat cameraMatrix_,
            MatOfDouble distortionCoefficients_,
            List<Mat> rvecs,
            List<Mat> tvecs,
            List<boolean[]> cornersUsed,
            double[] calobject_warp,
            Path imageSavePath) {
        // Clear the calibration image folder of any old images before we save the new ones.
        try {
            FileUtils.cleanDirectory(imageSavePath.toFile());
        } catch (Exception e) {
            logger.error("Failed to clean calibration image directory", e);
        }

        // For each observation, calc reprojection error
        List<BoardObservation> observations = new ArrayList<>();
        for (int snapshotId = 0; snapshotId < observationData.size(); snapshotId++) {
            // Copy object points to a new mat to allow warp modification without affecting underlying
            // data
            observationData.get(snapshotId).objectPoints.copyTo(objectPoints);

            List<Point> iPoints = observationData.get(snapshotId).imagePoints.toList();

            if (objectPoints.rows() != iPoints.size()) {
                var rows = objectPoints.rows();
                objectPoints.release();
                throw new RuntimeException(
                        "Objpts size ("
                                + rows
                                + ") != imgpts size ("
                                + iPoints.size()
                                + ") for snapshot "
                                + snapshotId
                                + "!");
            }

            // Apply warp, if set
            if (calobject_warp != null && calobject_warp.length == 2) {
                // mrcal warp model!
                // The chessboard spans [-1, 1] on the x and y axies. We then let
                // z=k_x(1-x^2)+k_y(1-y^2)

                double xmin = 0;
                double ymin = 0;
                double xmax = params.boardWidth * params.squareSize;
                double ymax = params.boardHeight * params.squareSize;
                double k_x = calobject_warp[0];
                double k_y = calobject_warp[1];

                // Convert to list, remap z, and back to cv::Mat
                var oPoints = objectPoints.toArray();
                for (var pt : oPoints) {
                    double x_norm = MathUtils.map(pt.x, xmin, xmax, -1, 1);
                    double y_norm = MathUtils.map(pt.y, ymin, ymax, -1, 1);
                    pt.z = k_x * (1 - x_norm * x_norm) + k_y * (1 - y_norm * y_norm);
                }
                objectPoints.fromArray(oPoints);
            }

            // Project distorted object points to image space
            try {
                Calib3d.projectPoints(
                        objectPoints,
                        rvecs.get(snapshotId),
                        tvecs.get(snapshotId),
                        cameraMatrix_,
                        distortionCoefficients_,
                        projectedMat);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // Calculate reprojection error for each point
            var reprojectionError = new ArrayList<Point>();
            var projectedPoints = projectedMat.toList();
            for (int j = 0; j < projectedPoints.size(); j++) {
                // Outliers are not part of the calibration, so don't calculate error for them
                if (!cornersUsed.get(snapshotId)[j]) {
                    continue;
                }

                // error = (measured - expected)
                var measured = projectedPoints.get(j);
                var expected = iPoints.get(j);

                // Sanity check -- negative corners make no sense here
                if (!(measured.x >= 0 && measured.y >= 0 && expected.x >= 0 && expected.y >= 0)) {
                    throw new RuntimeException(
                            "Negative corner in reprojection error calc! Measured: "
                                    + measured
                                    + ", expected: "
                                    + expected);
                }

                var error = new Point(measured.x - expected.x, measured.y - expected.y);
                reprojectionError.add(error);
            }

            var camToBoard = MathUtils.opencvRTtoPose3d(rvecs.get(snapshotId), tvecs.get(snapshotId));

            var inputImage = observationData.get(snapshotId).inputImage;
            Path image_path = null;
            String snapshotName = "img" + snapshotId + ".png";
            if (inputImage != null) {
                image_path = Paths.get(imageSavePath.toString(), snapshotName);
                Imgcodecs.imwrite(image_path.toString(), inputImage);
            }

            observations.add(
                    new BoardObservation(
                            objectPoints.toList(),
                            iPoints,
                            reprojectionError,
                            camToBoard,
                            cornersUsed.get(snapshotId),
                            snapshotName,
                            image_path));
        }

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

    @Override
    public void release() {
        stdDeviationsIntrinsics.release();
        stdDeviationsExtrinsics.release();
        perViewErrors.release();
        objectPoints.release();
        projectedMat.release();
    }
}

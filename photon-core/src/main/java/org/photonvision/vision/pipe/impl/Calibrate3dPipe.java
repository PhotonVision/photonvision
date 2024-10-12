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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.mrcal.MrCalJNI;
import org.photonvision.mrcal.MrCalJNI.MrCalResult;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.calibration.BoardObservation;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe.FindBoardCornersPipeResult;

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
                List<FindBoardCornersPipeResult> observations,
                FrameStaticProperties imageProps,
                Path imageSavePath) {
            this.observations = observations;
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

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing. In the format (Input image, object points, image points)
     * @return Result of processing.
     */
    @Override
    protected CameraCalibrationCoefficients process(CalibrationInput in) {
        var filteredIn =
                in.observations.stream()
                        .filter(
                                it ->
                                        it != null
                                                && it.imagePoints != null
                                                && it.objectPoints != null
                                                && it.size != null)
                        .collect(Collectors.toList());

        CameraCalibrationCoefficients ret;
        var start = System.nanoTime();

        if (MrCalJNILoader.getInstance().isLoaded() && params.useMrCal) {
            logger.debug("Calibrating with mrcal!");
            ret =
                    calibrateMrcal(
                            filteredIn,
                            in.imageProps.horizontalFocalLength,
                            in.imageProps.verticalFocalLength,
                            in.imageSavePath);
        } else {
            logger.debug("Calibrating with opencv!");
            ret =
                    calibrateOpenCV(
                            filteredIn,
                            in.imageProps.horizontalFocalLength,
                            in.imageProps.verticalFocalLength,
                            in.imageSavePath);
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
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in,
            double fxGuess,
            double fyGuess,
            Path imageSavePath) {
        List<MatOfPoint3f> objPointsIn =
                in.stream().map(it -> it.objectPoints).collect(Collectors.toList());
        List<MatOfPoint2f> imgPointsIn =
                in.stream().map(it -> it.imagePoints).collect(Collectors.toList());
        List<MatOfFloat> levelsArr = in.stream().map(it -> it.levels).collect(Collectors.toList());

        if (objPointsIn.size() != imgPointsIn.size() || objPointsIn.size() != levelsArr.size()) {
            logger.error("objpts.size != imgpts.size");
            return null;
        }

        // And delete rows depending on the level -- otherwise, level has no impact for opencv
        List<Mat> objPoints = new ArrayList<>();
        List<Mat> imgPoints = new ArrayList<>();
        for (int i = 0; i < objPointsIn.size(); i++) {
            MatOfPoint3f objPtsOut = new MatOfPoint3f();
            MatOfPoint2f imgPtsOut = new MatOfPoint2f();

            deleteIgnoredPoints(
                    objPointsIn.get(i), imgPointsIn.get(i), levelsArr.get(i), objPtsOut, imgPtsOut);

            objPoints.add(objPtsOut);
            imgPoints.add(imgPtsOut);
        }

        Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
        MatOfDouble distortionCoefficients = new MatOfDouble();
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        // initial camera matrix guess
        double cx = (in.get(0).size.width / 2.0) - 0.5;
        double cy = (in.get(0).size.width / 2.0) - 0.5;
        cameraMatrix.put(0, 0, new double[] {fxGuess, 0, cx, 0, fyGuess, cy, 0, 0, 1});

        try {
            // FindBoardCorners pipe outputs all the image points, object points, and frames
            // to calculate
            // imageSize from, other parameters are output Mats

            Calib3d.calibrateCameraExtended(
                    objPoints,
                    imgPoints,
                    new Size(in.get(0).size.width, in.get(0).size.height),
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
            return null;
        }

        JsonMatOfDouble cameraMatrixMat = JsonMatOfDouble.fromMat(cameraMatrix);
        JsonMatOfDouble distortionCoefficientsMat = JsonMatOfDouble.fromMat(distortionCoefficients);

        var observations =
                createObservations(
                        in, cameraMatrix, distortionCoefficients, rvecs, tvecs, null, imageSavePath);

        cameraMatrix.release();
        distortionCoefficients.release();
        rvecs.forEach(Mat::release);
        tvecs.forEach(Mat::release);
        objPoints.forEach(Mat::release);
        imgPoints.forEach(Mat::release);

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
            List<FindBoardCornersPipe.FindBoardCornersPipeResult> in,
            double fxGuess,
            double fyGuess,
            Path imageSavePath) {
        List<MatOfPoint2f> corner_locations =
                in.stream().map(it -> it.imagePoints).map(MatOfPoint2f::new).collect(Collectors.toList());

        List<MatOfFloat> levels =
                in.stream().map(it -> it.levels).map(MatOfFloat::new).collect(Collectors.toList());

        int imageWidth = (int) in.get(0).size.width;
        int imageHeight = (int) in.get(0).size.height;

        MrCalResult result =
                MrCalJNI.calibrateCamera(
                        corner_locations,
                        levels,
                        params.boardWidth,
                        params.boardHeight,
                        params.squareSize,
                        imageWidth,
                        imageHeight,
                        (fxGuess + fyGuess) / 2.0);

        levels.forEach(MatOfFloat::release);
        corner_locations.forEach(MatOfPoint2f::release);

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

        // Calculate optimized board poses manually. We get this for free from mrcal
        // too, but that's not JNIed (yet)
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        for (var o : in) {
            var rvec = new Mat();
            var tvec = new Mat();

            // If the calibration points contain points that are negative then we need to exclude them,
            // they are considered points that we dont want to use in calibration/solvepnp. These points
            // are required prior to this to allow mrcal to work.
            Point3[] oPoints = o.objectPoints.toArray();
            Point[] iPoints = o.imagePoints.toArray();

            List<Point3> outputOPoints = new ArrayList<Point3>();
            List<Point> outputIPoints = new ArrayList<Point>();

            for (int i = 0; i < iPoints.length; i++) {
                if (iPoints[i].x >= 0 && iPoints[i].y >= 0) {
                    outputIPoints.add(iPoints[i]);
                }
            }
            for (int i = 0; i < oPoints.length; i++) {
                if (oPoints[i].x >= 0 && oPoints[i].y >= 0 && oPoints[i].z >= 0) {
                    outputOPoints.add(oPoints[i]);
                }
            }

            o.objectPoints.fromList(outputOPoints);
            o.imagePoints.fromList(outputIPoints);

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
                        new double[] {result.warp_x, result.warp_y},
                        imageSavePath);

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
            double[] calobject_warp,
            Path imageSavePath) {
        List<Mat> objPoints = in.stream().map(it -> it.objectPoints).collect(Collectors.toList());
        List<Mat> imgPts = in.stream().map(it -> it.imagePoints).collect(Collectors.toList());

        // Clear the calibration image folder of any old images before we save the new ones.

        try {
            FileUtils.cleanDirectory(imageSavePath.toFile());
        } catch (Exception e) {
            logger.error("Failed to clean calibration image directory", e);
        }

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
                // The chessboard spans [-1, 1] on the x and y axies. We then let
                // z=k_x(1-x^2)+k_y(1-y^2)

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

            var inputImage = in.get(i).inputImage;
            Path image_path = null;
            String snapshotName = "img" + i + ".png";
            if (inputImage != null) {
                image_path = Paths.get(imageSavePath.toString(), snapshotName);
                Imgcodecs.imwrite(image_path.toString(), inputImage);
            }

            observations.add(
                    new BoardObservation(
                            i_objPts, i_imgPts, reprojectionError, camToBoard, true, snapshotName, image_path));
        }
        jac_temp.release();

        return observations;
    }

    /** Delete all rows of mats where level is < 0. Useful for opencv */
    private void deleteIgnoredPoints(
            MatOfPoint3f objPtsMatIn,
            MatOfPoint2f imgPtsMatIn,
            MatOfFloat levelsMat,
            MatOfPoint3f objPtsMatOut,
            MatOfPoint2f imgPtsMatOut) {
        var levels = levelsMat.toArray();
        var objPtsIn = objPtsMatIn.toArray();
        var imgPtsIn = imgPtsMatIn.toArray();

        var objPtsOut = new ArrayList<Point3>();
        var imgPtsOut = new ArrayList<Point>();

        for (int i = 0; i < levels.length; i++) {
            if (levels[i] >= 0) {
                // point survives
                objPtsOut.add(objPtsIn[i]);
                imgPtsOut.add(imgPtsIn[i]);
            }
        }

        objPtsMatOut.fromList(objPtsOut);
        imgPtsMatOut.fromList(imgPtsOut);
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

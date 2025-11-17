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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.util.Units;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.UICalibrationData.BoardType;
import org.photonvision.vision.pipeline.UICalibrationData.TagFamily;

public class Calibrate3dPipeTest {
    @BeforeAll
    public static void init() throws IOException {
        TestUtils.loadLibraries();
        TestUtils.loadMrcal();

        var logLevel = LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.Config, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
    }

    enum CalibrationDatasets {
        SQUARES_LIFECAM_480(
                "lifecam/2024-01-02_lifecam_480",
                new Size(640, 480),
                new Size(11, 11),
                BoardType.CHESSBOARD,
                false),
        SQUARES_LIFECAM_1280(
                "lifecam/2024-01-02_lifecam_1280",
                new Size(1280, 720),
                new Size(11, 11),
                BoardType.CHESSBOARD,
                false),
        CHARUCO_LIFECAM_480(
                "lifecam/2024-05-07_lifecam_480",
                new Size(640, 480),
                new Size(8, 8),
                BoardType.CHARUCOBOARD,
                false),
        CHARUCO_LIFECAM_1280(
                "lifecam/2024-05-07_lifecam_1280",
                new Size(1280, 720),
                new Size(8, 8),
                BoardType.CHARUCOBOARD,
                false),
        CHARUCO_OLDPATTERN_LIFECAM_480(
                "lifecam/2024-06-19_lifecam_480_Old_Pattern",
                new Size(640, 480),
                new Size(8, 8),
                BoardType.CHARUCOBOARD,
                true),
        CHARUCO_OLDPATTERN_LIFECAM_1280(
                "lifecam/2024-06-19_lifecam_1280_Old_Pattern",
                new Size(1280, 720),
                new Size(8, 8),
                BoardType.CHARUCOBOARD,
                true);

        final String path;
        final Size size;
        final Size boardSize;
        final BoardType boardType;
        final boolean useOldPattern;

        private CalibrationDatasets(
                String path, Size image, Size chessboard, BoardType boardType, boolean useOldPattern) {
            this.path = path;
            this.size = image;
            this.boardSize = chessboard;
            this.boardType = boardType;
            this.useOldPattern = useOldPattern;
        }
    }

    /**
     * Run camera calibration on a given dataset
     *
     * @param dataset Location of images and their size
     * @param useMrCal If we should use mrcal or opencv for camera calibration
     */
    @CartesianTest
    public void calibrateTestMatrix(
            @Enum(CalibrationDatasets.class) CalibrationDatasets dataset,
            @Values(booleans = {true, false}) boolean useMrCal) {
        // Pi3 and V1.3 camera
        String squareBase = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        String charucoBase = TestUtils.getCharucoBoardImagesPath().toAbsolutePath().toString();

        File squareDir = Path.of(squareBase, dataset.path).toFile();
        File charucoDir = Path.of(charucoBase, dataset.path).toFile();

        if (dataset.boardType == BoardType.CHESSBOARD)
            calibrateCommon(
                    dataset.size,
                    squareDir,
                    dataset.boardSize,
                    dataset.boardType,
                    useMrCal,
                    dataset.useOldPattern);
        else if (dataset.boardType == BoardType.CHARUCOBOARD)
            calibrateCommon(
                    dataset.size,
                    charucoDir,
                    dataset.boardSize,
                    dataset.boardType,
                    useMrCal,
                    dataset.useOldPattern);
    }

    public static void calibrateCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            BoardType boardType,
            boolean useMrCal,
            boolean useOldPattern) {
        calibrateCommon(
                imgRes,
                rootFolder,
                boardDim,
                Units.inchesToMeters(1),
                Units.inchesToMeters(0.75),
                boardType,
                TagFamily.Dict_4X4_1000,
                imgRes.width / 2,
                imgRes.height / 2,
                useMrCal,
                useOldPattern);
    }

    public static void calibrateCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            double markerSize,
            BoardType boardType,
            TagFamily tagFamily,
            double expectedXCenter,
            double expectedYCenter,
            boolean useMrCal,
            boolean useOldPattern) {
        calibrateCommon(
                imgRes,
                rootFolder,
                boardDim,
                Units.inchesToMeters(1),
                markerSize,
                boardType,
                tagFamily,
                expectedXCenter,
                expectedYCenter,
                useMrCal,
                useOldPattern);
    }

    public static void calibrateCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            double boardGridSize_m,
            double markerSize,
            BoardType boardType,
            TagFamily tagFamily,
            double expectedXCenter,
            double expectedYCenter,
            boolean useMrCal,
            boolean useOldPattern) {
        int startMatCount = CVMat.getMatCount();

        File[] directoryListing = rootFolder.listFiles();

        assertTrue(directoryListing.length >= 12);

        Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline(10);
        calibration3dPipeline.getSettings().boardType = boardType;
        calibration3dPipeline.getSettings().markerSize = markerSize;
        calibration3dPipeline.getSettings().tagFamily = tagFamily;
        calibration3dPipeline.getSettings().resolution = imgRes;
        calibration3dPipeline.getSettings().boardHeight = (int) Math.round(boardDim.height);
        calibration3dPipeline.getSettings().boardWidth = (int) Math.round(boardDim.width);
        calibration3dPipeline.getSettings().gridSize = boardGridSize_m;
        calibration3dPipeline.getSettings().streamingFrameDivisor = FrameDivisor.NONE;
        calibration3dPipeline.getSettings().useMrCal = useMrCal;
        calibration3dPipeline.getSettings().useOldPattern = useOldPattern;

        for (var file : directoryListing) {
            if (file.isFile()) {
                calibration3dPipeline.takeSnapshot();
                var frame =
                        new Frame(
                                0,
                                new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                new CVMat(),
                                FrameThresholdType.NONE,
                                new FrameStaticProperties((int) imgRes.width, (int) imgRes.height, 67, null));
                var output = calibration3dPipeline.run(frame, QuirkyCamera.DefaultCamera);

                // TestUtils.showImage(output.inputAndOutputFrame.processedImage.getMat(),
                // file.getName(),
                // 1);
                output.release();
                frame.release();
            }
        }

        assertTrue(
                calibration3dPipeline.foundCornersList.stream()
                        .map(it -> it.imagePoints)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));

        var cal =
                calibration3dPipeline.tryCalibration(
                        ConfigManager.getInstance()
                                .getCalibrationImageSavePathWithRes(imgRes, "Calibration_Test"));
        calibration3dPipeline.finishCalibration();

        // visuallyDebugDistortion(directoryListing, imgRes, cal );

        // Confirm we have indeed gotten valid calibration objects
        assertNotNull(cal);
        assertNotNull(cal.observations);

        // Confirm the calibrated center pixel is fairly close to of the "expected"
        // location at the
        // center of the sensor.
        // For all our data samples so far, this should be true.
        double centerXErrPct =
                Math.abs(cal.cameraIntrinsics.data[2] - expectedXCenter) / (expectedXCenter) * 100.0;
        double centerYErrPct =
                Math.abs(cal.cameraIntrinsics.data[5] - expectedYCenter) / (expectedYCenter) * 100.0;
        assertTrue(centerXErrPct < 10.0);
        assertTrue(centerYErrPct < 10.0);

        System.out.println("Camera Intrinsics: " + cal.cameraIntrinsics.toString());
        System.out.println("Dist Coeffs: " + cal.distCoeffs.toString());

        // Confirm we didn't get leaky on our mat usage
        // assertEquals(startMatCount, CVMat.getMatCount()); // TODO Figure out why this
        // doesn't
        // work in CI
        System.out.println("CVMats left: " + CVMat.getMatCount() + " Start: " + startMatCount);
    }

    /**
     * Uses a given camera coefficients matrix set to "undistort" every image file found in a given
     * directory and display them. Provides an easy way to visually debug the results of the
     * calibration routine. Seems to play havoc with CI and takes a chunk of time, so shouldn't
     * usually be left active in tests.
     *
     * @param directoryListing
     * @param imgRes
     * @param cal
     */
    @SuppressWarnings("unused")
    private void visuallyDebugDistortion(
            File[] directoryListing, Size imgRes, CameraCalibrationCoefficients cal) {
        for (var file : directoryListing) {
            if (file.isFile()) {
                Mat raw = Imgcodecs.imread(file.getAbsolutePath());
                Mat undistorted = new Mat(new Size(imgRes.width * 2, imgRes.height * 2), raw.type());
                Calib3d.undistort(
                        raw,
                        undistorted,
                        cal.cameraIntrinsics.getAsMatOfDouble(),
                        cal.distCoeffs.getAsMatOfDouble());
                TestUtils.showImage(undistorted, "undistorted " + file.getName(), 1);
                raw.release();
                undistorted.release();
            }
        }
    }
}

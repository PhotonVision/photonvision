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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;

public class Calibrate3dPipeTest {
    @BeforeAll
    public static void init() throws IOException {
        TestUtils.loadLibraries();
        MrCalJNILoader.forceLoad();

        var logLevel = LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.Config, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
    }

    enum CalibrationDatasets {
        LIFECAM_480("lifecam/2024-01-02_lifecam_480", new Size(640, 480), new Size(11, 11)),
        LIFECAM_1280("lifecam/2024-01-02_lifecam_1280", new Size(1280, 720), new Size(11, 11));

        final String path;
        final Size size;
        final Size boardSize;

        private CalibrationDatasets(String path, Size image, Size chessboard) {
            this.path = path;
            this.size = image;
            this.boardSize = chessboard;
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
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, dataset.path).toFile();
        calibrateSquaresCommon(dataset.size, dir, dataset.boardSize, useMrCal);
    }

    public static void calibrateSquaresCommon(
            Size imgRes, File rootFolder, Size boardDim, boolean useMrCal) {
        calibrateSquaresCommon(
                imgRes,
                rootFolder,
                boardDim,
                Units.inchesToMeters(1),
                imgRes.width / 2,
                imgRes.height / 2,
                useMrCal);
    }

    public static void calibrateSquaresCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            double expectedXCenter,
            double expectedYCenter,
            boolean useMrCal) {
        calibrateSquaresCommon(
                imgRes,
                rootFolder,
                boardDim,
                Units.inchesToMeters(1),
                expectedXCenter,
                expectedYCenter,
                useMrCal);
    }

    public static void calibrateSquaresCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            double boardGridSize_m,
            double expectedXCenter,
            double expectedYCenter,
            boolean useMrCal) {
        int startMatCount = CVMat.getMatCount();

        File[] directoryListing = rootFolder.listFiles();

        assertTrue(directoryListing.length >= 12);

        Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline(10, "test_squares_common");
        calibration3dPipeline.getSettings().boardType = UICalibrationData.BoardType.CHESSBOARD;
        calibration3dPipeline.getSettings().resolution = imgRes;
        calibration3dPipeline.getSettings().boardHeight = (int) Math.round(boardDim.height);
        calibration3dPipeline.getSettings().boardWidth = (int) Math.round(boardDim.width);
        calibration3dPipeline.getSettings().gridSize = boardGridSize_m;
        calibration3dPipeline.getSettings().streamingFrameDivisor = FrameDivisor.NONE;
        calibration3dPipeline.getSettings().useMrCal = useMrCal;

        for (var file : directoryListing) {
            if (file.isFile()) {
                calibration3dPipeline.takeSnapshot();
                var frame =
                        new Frame(
                                new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                new CVMat(),
                                FrameThresholdType.NONE,
                                new FrameStaticProperties((int) imgRes.width, (int) imgRes.height, 67, null));
                var output = calibration3dPipeline.run(frame, QuirkyCamera.DefaultCamera);

                // TestUtils.showImage(output.inputAndOutputFrame.processedImage.getMat(), file.getName(),
                // 1);
                output.release();
                frame.release();
            }
        }

        assertTrue(
                calibration3dPipeline.foundCornersList.stream()
                        .map(it -> it.imagePoints)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));

        var cal = calibration3dPipeline.tryCalibration();
        calibration3dPipeline.finishCalibration();

        // visuallyDebugDistortion(directoryListing, imgRes, cal );

        // Confirm we have indeed gotten valid calibration objects
        assertNotNull(cal);
        assertNotNull(cal.observations);

        // Confirm the calibrated center pixel is fairly close to of the "expected" location at the
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
        // assertEquals(startMatCount, CVMat.getMatCount()); // TODO Figure out why this doesn't
        // work in CI
        System.out.println("CVMats left: " + CVMat.getMatCount() + " Start: " + startMatCount);
    }

    /**
     * Uses a given camera coefficents matrix set to "undistort" every image file found in a given
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
                        raw, undistorted, cal.cameraIntrinsics.getAsMat(), cal.distCoeffs.getAsMat());
                TestUtils.showImage(undistorted, "undistorted " + file.getName(), 1);
                raw.release();
                undistorted.release();
            }
        }
    }
}

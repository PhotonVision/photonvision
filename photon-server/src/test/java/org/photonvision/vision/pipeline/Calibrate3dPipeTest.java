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

import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.util.Units;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;

public class Calibrate3dPipeTest {
    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void perViewErrorsTest() {
        List<Mat> frames = new ArrayList<>();

        File dir = new File(TestUtils.getDotBoardImagesPath().toAbsolutePath().toString());
        File[] directoryListing = dir.listFiles();
        for (var file : directoryListing) {
            frames.add(Imgcodecs.imread(file.getAbsolutePath()));
        }

        FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
        findBoardCornersPipe.setParams(
                new FindBoardCornersPipe.FindCornersPipeParams(
                        11, 4, UICalibrationData.BoardType.DOTBOARD, 15));

        List<Triple<Size, Mat, Mat>> foundCornersList = new ArrayList<>();

        for (var f : frames) {
            var copy = new Mat();
            f.copyTo(copy);
            foundCornersList.add(findBoardCornersPipe.run(Pair.of(f, copy)).output);
        }

        Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
        calibrate3dPipe.setParams(new Calibrate3dPipe.CalibratePipeParams(new Size(640, 480)));

        var calibrate3dPipeOutput = calibrate3dPipe.run(foundCornersList);
        assertTrue(calibrate3dPipeOutput.output.perViewErrors.length > 0);
        System.out.println(
                "Per View Errors: " + Arrays.toString(calibrate3dPipeOutput.output.perViewErrors));
    }

    @Test
    public void calibrationPipelineTest() {

        int startMatCount = CVMat.getMatCount();

        File dir = new File(TestUtils.getDotBoardImagesPath().toAbsolutePath().toString());
        File[] directoryListing = dir.listFiles();

        Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline(20);
        calibration3dPipeline.getSettings().boardHeight = 11;
        calibration3dPipeline.getSettings().boardWidth = 4;
        calibration3dPipeline.getSettings().boardType = UICalibrationData.BoardType.DOTBOARD;
        calibration3dPipeline.getSettings().gridSize = 15;
        calibration3dPipeline.getSettings().resolution = new Size(640, 480);

        for (var file : directoryListing) {
            calibration3dPipeline.takeSnapshot();
            var output =
                    calibration3dPipeline.run(
                            new Frame(
                                    new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                    new FrameStaticProperties(640, 480, 60, new Rotation2d(), null)),
                            QuirkyCamera.DefaultCamera);
            // TestUtils.showImage(output.outputFrame.image.getMat());
            output.release();
        }

        assertTrue(
                calibration3dPipeline.foundCornersList.stream()
                        .map(Triple::getRight)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));

        calibration3dPipeline.removeSnapshot(0);
        calibration3dPipeline
                .run(
                        new Frame(
                                new CVMat(Imgcodecs.imread(directoryListing[0].getAbsolutePath())),
                                new FrameStaticProperties(640, 480, 60, new Rotation2d(), null)),
                        QuirkyCamera.DefaultCamera)
                .release();

        assertTrue(
                calibration3dPipeline.foundCornersList.stream()
                        .map(Triple::getRight)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));

        var cal = calibration3dPipeline.tryCalibration();
        calibration3dPipeline.finishCalibration();

        assertNotNull(cal);
        assertNotNull(cal.perViewErrors);
        System.out.println("Per View Errors: " + Arrays.toString(cal.perViewErrors));
        System.out.println("Camera Intrinsics : " + cal.cameraIntrinsics.toString());
        System.out.println("Camera Extrinsics : " + cal.cameraExtrinsics.toString());
        System.out.println("Standard Deviation: " + cal.standardDeviation);
        System.out.println(
                "Mean: " + Arrays.stream(calibration3dPipeline.perViewErrors()).average().toString());

        // Confirm we didn't get leaky on our mat usage
        assertTrue(CVMat.getMatCount() == startMatCount);
    }

    @Test
    public void calibrateSquares320x240_pi() {
        // Pi3 and V1.3 camera
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "piCam", "320_240_1").toFile();
        Size sz = new Size(320, 240);
        calibrateSquaresCommon(sz, dir);
    }

    @Test
    public void calibrateSquares640x480_pi() {
        // Pi3 and V1.3 camera
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "piCam", "640_480_1").toFile();
        Size sz = new Size(640, 480);
        calibrateSquaresCommon(sz, dir);
    }

    @Test
    public void calibrateSquares960x720_pi() {
        // Pi3 and V1.3 camera
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "piCam", "960_720_1").toFile();
        Size sz = new Size(960, 720);
        calibrateSquaresCommon(sz, dir);
    }

    @Test
    public void calibrateSquares1920x1080_pi() {
        // Pi3 and V1.3 camera
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "piCam", "1920_1080_1").toFile();
        Size sz = new Size(1920, 1080);
        calibrateSquaresCommon(sz, dir);
    }

    @Test
    public void calibrateSquares320x240_gloworm() {
        // Gloworm Beta
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "gloworm", "320_240_1").toFile();
        Size sz = new Size(320, 240);
        Size boardDim = new Size(9, 7);
        calibrateSquaresCommon(sz, dir, boardDim);
    }

    @Test
    public void calibrateSquares_960_720_gloworm() {
        // Gloworm Beta
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "gloworm", "960_720_1").toFile();
        Size sz = new Size(960, 720);
        Size boardDim = new Size(9, 7);
        calibrateSquaresCommon(sz, dir, boardDim);
    }

    @Test
    public void calibrateSquares_1280_720_gloworm() {
        // Gloworm Beta
        // This image set will return a fairly offset Y-pixel for the optical center point
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "gloworm", "1280_720_1").toFile();
        Size sz = new Size(1280, 720);
        Size boardDim = new Size(9, 7);
        calibrateSquaresCommon(sz, dir, boardDim, 640, 192);
    }

    @Test
    public void calibrateSquares_1920_1080_gloworm() {
        // Gloworm Beta
        // This image set has most samples on the right, and is expected to return a slightly
        // wonky calibration.
        String base = TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString();
        File dir = Path.of(base, "gloworm", "1920_1080_1").toFile();
        Size sz = new Size(1920, 1080);
        Size boardDim = new Size(9, 7);
        calibrateSquaresCommon(sz, dir, boardDim, 1311, 540);
    }

    public void calibrateSquaresCommon(Size imgRes, File rootFolder) {
        calibrateSquaresCommon(imgRes, rootFolder, new Size(8, 8));
    }

    public void calibrateSquaresCommon(Size imgRes, File rootFolder, Size boardDim) {
        calibrateSquaresCommon(
                imgRes, rootFolder, boardDim, Units.inchesToMeters(1), imgRes.width / 2, imgRes.height / 2);
    }

    public void calibrateSquaresCommon(
            Size imgRes, File rootFolder, Size boardDim, double expectedXCenter, double expectedYCenter) {
        calibrateSquaresCommon(
                imgRes, rootFolder, boardDim, Units.inchesToMeters(1), expectedXCenter, expectedYCenter);
    }

    public void calibrateSquaresCommon(
            Size imgRes,
            File rootFolder,
            Size boardDim,
            double boardGridSize_m,
            double expectedXCenter,
            double expectedYCenter) {

        int startMatCount = CVMat.getMatCount();

        File[] directoryListing = rootFolder.listFiles();

        assertTrue(directoryListing.length >= 25);

        Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline(20);
        calibration3dPipeline.getSettings().boardType = UICalibrationData.BoardType.CHESSBOARD;
        calibration3dPipeline.getSettings().resolution = imgRes;
        calibration3dPipeline.getSettings().boardHeight = (int) Math.round(boardDim.height);
        calibration3dPipeline.getSettings().boardWidth = (int) Math.round(boardDim.width);
        calibration3dPipeline.getSettings().gridSize = boardGridSize_m;

        for (var file : directoryListing) {
            if (file.isFile()) {
                calibration3dPipeline.takeSnapshot();
                var output =
                        calibration3dPipeline.run(
                                new Frame(
                                        new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                        new FrameStaticProperties(
                                                (int) imgRes.width, (int) imgRes.height, 67, new Rotation2d(), null)),
                                QuirkyCamera.DefaultCamera);

                // TestUtils.showImage(output.outputFrame.image.getMat(), file.getName(), 1);
                output.outputFrame.release();
            }
        }

        assertTrue(
                calibration3dPipeline.foundCornersList.stream()
                        .map(Triple::getRight)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));

        var cal = calibration3dPipeline.tryCalibration();
        calibration3dPipeline.finishCalibration();

        // visuallyDebugDistortion(directoryListing, imgRes, cal );

        // Confirm we have indeed gotten valid calibration objects
        assertNotNull(cal);
        assertNotNull(cal.perViewErrors);

        // Confirm the calibrated center pixel is fairly close to of the "expected" location at the
        // center of the sensor.
        // For all our data samples so far, this should be true.
        double centerXErrPct =
                Math.abs(cal.cameraIntrinsics.data[2] - expectedXCenter) / (expectedXCenter) * 100.0;
        double centerYErrPct =
                Math.abs(cal.cameraIntrinsics.data[5] - expectedYCenter) / (expectedYCenter) * 100.0;
        assertTrue(centerXErrPct < 10.0);
        assertTrue(centerYErrPct < 10.0);

        System.out.println("Per View Errors: " + Arrays.toString(cal.perViewErrors));
        System.out.println("Camera Intrinsics : " + cal.cameraIntrinsics.toString());
        System.out.println("Camera Extrinsics : " + cal.cameraExtrinsics.toString());
        System.out.println("Standard Deviation: " + cal.standardDeviation);
        System.out.println(
                "Mean: " + Arrays.stream(calibration3dPipeline.perViewErrors()).average().toString());

        // Confirm we didn't get leaky on our mat usage
        assertTrue(CVMat.getMatCount() == startMatCount);
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
                Imgproc.undistort(
                        raw, undistorted, cal.cameraIntrinsics.getAsMat(), cal.cameraExtrinsics.getAsMat());
                TestUtils.showImage(undistorted, "undistorted " + file.getName(), 1);
                raw.release();
                undistorted.release();
            }
        }
    }
}

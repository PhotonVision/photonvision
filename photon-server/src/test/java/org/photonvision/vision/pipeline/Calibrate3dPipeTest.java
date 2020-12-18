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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.io.File;
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
                findBoardCornersPipe.setParams(new FindBoardCornersPipe.FindCornersPipeParams(11, 4,
                                UICalibrationData.BoardType.DOTBOARD, 15));

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
                System.out.println("Per View Errors: " + Arrays.toString(calibrate3dPipeOutput.output.perViewErrors));
        }

        @Test
        public void calibrationPipelineTest() {

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
                        var output = calibration3dPipeline.run(new Frame(
                                        new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                        new FrameStaticProperties(640, 480, 60, new Rotation2d(), null)));
                        // TestUtils.showImage(output.outputFrame.image.getMat());
                }

                assertTrue(calibration3dPipeline.foundCornersList.stream().map(Triple::getRight)
                                .allMatch(it -> it.width() > 0 && it.height() > 0));

                calibration3dPipeline.removeSnapshot(0);
                calibration3dPipeline.run(new Frame(new CVMat(Imgcodecs.imread(directoryListing[0].getAbsolutePath())),
                                new FrameStaticProperties(640, 480, 60, new Rotation2d(), null)));

                assertTrue(calibration3dPipeline.foundCornersList.stream().map(Triple::getRight)
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
        }

        @Test
        public void calibrateSquaresTest() {

                File dir = new File(TestUtils.getSquaresBoardImagesPath().toAbsolutePath().toString());
                File[] directoryListing = dir.listFiles();

                Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline(20);
                calibration3dPipeline.getSettings().boardType = UICalibrationData.BoardType.CHESSBOARD;
                calibration3dPipeline.getSettings().resolution = new Size(320, 240);

                for (var file : directoryListing) {
                        if (file.isFile()) {
                                calibration3dPipeline.takeSnapshot();
                                var output = calibration3dPipeline.run(new Frame(
                                                new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                                new FrameStaticProperties(320, 240, 67, new Rotation2d(), null)));

                                //TestUtils.showImage(output.outputFrame.image.getMat(), file.getName());
                        }
                }

                assertTrue(calibration3dPipeline.foundCornersList.stream().map(Triple::getRight)
                                .allMatch(it -> it.width() > 0 && it.height() > 0));

                var cal = calibration3dPipeline.tryCalibration();
                calibration3dPipeline.finishCalibration();

                for (var file : directoryListing) {
                        if (file.isFile()) {
                                Mat raw = Imgcodecs.imread(file.getAbsolutePath());
                                Mat undistorted = new Mat(new Size(600,600), raw.type());
                                Imgproc.undistort(raw, undistorted, cal.cameraIntrinsics.getAsMat(), cal.cameraExtrinsics.getAsMat());
                            
                                TestUtils.showImage(undistorted, "undistorted " + file.getName(), 1);
                        }
                }
            

        assertNotNull(cal);
        assertNotNull(cal.perViewErrors);
        System.out.println("Per View Errors: " + Arrays.toString(cal.perViewErrors));
        System.out.println("Camera Intrinsics : " + cal.cameraIntrinsics.toString());
        System.out.println("Camera Extrinsics : " + cal.cameraExtrinsics.toString());
        System.out.println("Standard Deviation: " + cal.standardDeviation);
        System.out.println(
                "Mean: " + Arrays.stream(calibration3dPipeline.perViewErrors()).average().toString());
    }

}

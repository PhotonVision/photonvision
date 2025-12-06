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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.SolvePNPPipe;
import org.photonvision.vision.pipe.impl.SolvePNPPipe.SolvePNPPipeParams;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class CalibrationRotationPipeTest {
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
        ConfigManager.getInstance().load();
    }

    @Test
    public void meme() {
        var s = new Size(200, 100);

        var p = new Point(2, 1);

        {
            var angle = ImageRotationMode.DEG_90_CCW;
            var expected = new Point(p.y, s.width - p.x);
            var rotatedP = angle.rotatePoint(p, s.width, s.height);
            assertEquals(expected.x, rotatedP.x, 1e-6);
            assertEquals(expected.y, rotatedP.y, 1e-6);
        }
        {
            var angle = ImageRotationMode.DEG_180_CCW;
            var expected = new Point(s.width - p.x, s.height - p.y);
            var rotatedP = angle.rotatePoint(p, s.width, s.height);
            assertEquals(expected.x, rotatedP.x, 1e-6);
            assertEquals(expected.y, rotatedP.y, 1e-6);
        }
        {
            var angle = ImageRotationMode.DEG_270_CCW;
            var expected = new Point(s.height - p.y, p.x);
            var rotatedP = angle.rotatePoint(p, s.width, s.height);
            assertEquals(expected.x, rotatedP.x, 1e-6);
            assertEquals(expected.y, rotatedP.y, 1e-6);
        }
    }

    @CartesianTest
    public void testUndistortImagePointsWithRotation(@Enum ImageRotationMode rot) {
        if (rot == ImageRotationMode.DEG_0) {
            return;
        }

        CameraCalibrationCoefficients coeffs =
                new CameraCalibrationCoefficients(
                        new Size(1270, 720),
                        new JsonMatOfDouble(
                                3,
                                3,
                                new double[] {
                                    900, 0, 500,
                                    0, 951, 321,
                                    0, 0, 1
                                }),
                        new JsonMatOfDouble(
                                1,
                                8,
                                new double[] {
                                    0.25,
                                    -1.5,
                                    0.0017808248356550637,
                                    .00004,
                                    2.179764689221826,
                                    -0.034952777924711353,
                                    0.09625562194891251,
                                    -0.1860797479660746
                                }),
                        new double[0],
                        List.of(),
                        new Size(),
                        1,
                        CameraLensModel.LENSMODEL_OPENCV);

        FrameStaticProperties frameProps =
                new FrameStaticProperties(
                        (int) coeffs.unrotatedImageSize.width,
                        (int) coeffs.unrotatedImageSize.height,
                        66,
                        coeffs);

        FrameStaticProperties rotatedFrameProps = frameProps.rotate(rot);

        Point[] originalPoints = {new Point(100, 100), new Point(200, 200), new Point(300, 100)};

        // Distort the original points
        var distortedOriginalPoints =
                OpenCVHelp.distortPoints(
                        List.of(originalPoints),
                        frameProps.cameraCalibration.getCameraIntrinsicsMat(),
                        frameProps.cameraCalibration.getDistCoeffsMat());

        // and rotate them once distorted
        var rotatedDistortedPoints =
                distortedOriginalPoints.stream()
                        .map(it -> rot.rotatePoint(it, frameProps.imageWidth, frameProps.imageHeight))
                        .toList();

        // Now let's instead rotate then distort
        var rotatedOriginalPoints =
                Arrays.stream(originalPoints)
                        .map(it -> rot.rotatePoint(it, frameProps.imageWidth, frameProps.imageHeight))
                        .toList();

        var distortedRotatedPoints =
                OpenCVHelp.distortPoints(
                        rotatedOriginalPoints,
                        rotatedFrameProps.cameraCalibration.getCameraIntrinsicsMat(),
                        rotatedFrameProps.cameraCalibration.getDistCoeffsMat());

        System.out.println("Rotated distorted: " + rotatedDistortedPoints.toString());
        System.out.println("Distorted rotated: " + distortedRotatedPoints.toString());

        for (int i = 0; i < distortedRotatedPoints.size(); i++) {
            assertEquals(rotatedDistortedPoints.get(i).x, distortedRotatedPoints.get(i).x, 1e-6);
            assertEquals(rotatedDistortedPoints.get(i).y, distortedRotatedPoints.get(i).y, 1e-6);
        }
    }

    @Test
    public void testRotateCoefficients180multiple() {
        ImageRotationMode rot = ImageRotationMode.DEG_180_CCW;

        // GIVEN A camera calibration
        var res = new Size(1270, 720);
        double fx = 900;
        double fy = 951;
        double cx = 500;
        double cy = 321;
        double[] intrinsics = {fx, 0, cx, 0, fy, cy, 0, 0, 1};
        double[] distCoeffs = {
            0.25,
            -1.5,
            0.0017808248356550637,
            .00004,
            2.179764689221826,
            -0.034952777924711353,
            0.09625562194891251,
            -0.1860797479660746
        };
        CameraCalibrationCoefficients coeffs =
                new CameraCalibrationCoefficients(
                        res,
                        new JsonMatOfDouble(3, 3, intrinsics),
                        new JsonMatOfDouble(1, 8, distCoeffs),
                        new double[] {},
                        List.of(),
                        new Size(),
                        1,
                        CameraLensModel.LENSMODEL_OPENCV);

        // WHEN the camera calibration is rotated 180 degrees
        var coeffs2 = coeffs.rotateCoefficients(rot);

        // THEN the optical center should be rotated 180 degrees
        double[] rotatedCamMat = {fx, 0, res.width - cx, 0, fy, res.height - cy, 0, 0, 1};
        assertArrayEquals(rotatedCamMat, coeffs2.cameraIntrinsics.data);
        // AND the image size should be the same
        assertEquals(res, coeffs2.unrotatedImageSize);

        // WHEN the camera calibration is rotated 180 degrees
        var coeffs3 = coeffs2.rotateCoefficients(rot);

        // THEN the camera matrix will be the same as the original
        assertArrayEquals(intrinsics, coeffs3.cameraIntrinsics.data);
        // AND the image size should be the same
        assertEquals(res, coeffs2.unrotatedImageSize);
    }

    @CartesianTest
    public void testCalibrationDataIsValidWithRotation(@Enum ImageRotationMode rot) {
        double[] intrinsics = {
            900, 0, 500,
            0, 951, 321,
            0, 0, 1
        };
        double[] distCoeffs = {
            0.25,
            -1.5,
            0.0017808248356550637,
            .00004,
            2.179764689221826,
            -0.034952777924711353,
            0.09625562194891251,
            -0.1860797479660746
        };
        // GIVEN A camera calibration
        CameraCalibrationCoefficients coeffs =
                new CameraCalibrationCoefficients(
                        new Size(1270, 720),
                        new JsonMatOfDouble(3, 3, intrinsics),
                        new JsonMatOfDouble(1, 8, distCoeffs),
                        new double[] {},
                        List.of(),
                        new Size(),
                        1,
                        CameraLensModel.LENSMODEL_OPENCV);
        // WHEN A camera calibration is rotated 4 times
        for (int i = 0; i < 4; i++) {
            coeffs = coeffs.rotateCoefficients(rot);
        }
        // THEN, it should be like it was never rotated at all
        assertArrayEquals(intrinsics, coeffs.cameraIntrinsics.data);
        assertArrayEquals(distCoeffs, coeffs.distCoeffs.data);
    }

    @Test
    public void testApriltagRotated() {
        // matt's lifecam
        CameraCalibrationCoefficients coeffs =
                new CameraCalibrationCoefficients(
                        new Size(1270, 720),
                        new JsonMatOfDouble(
                                3,
                                3,
                                new double[] {
                                    1132.983599412085, 0.0, 610.3195830765223,
                                    0.0, 1138.2884596791835, 346.4121207400337,
                                    0.0, 0.0, 1.0
                                }),
                        new JsonMatOfDouble(
                                1,
                                8,
                                new double[] {
                                    0.11508197558262527,
                                    -1.158603446817735,
                                    0.0017808248356550637,
                                    4.3915976993589873E-4,
                                    2.179764689221826,
                                    -0.034952777924711353,
                                    0.04625562194891251,
                                    -0.0860797479660746
                                }),
                        new double[0],
                        List.of(),
                        new Size(),
                        1,
                        CameraLensModel.LENSMODEL_OPENCV);

        // Matt's lifecam pointing at a wall
        var distortedCorners =
                List.of(
                        new Point(834.702271, 338.878143),
                        new Point(1011.808899, 345.824463),
                        new Point(964.300476, 225.330795),
                        new Point(803.971191, 217.359055));

        SolvePNPPipe pipe = new SolvePNPPipe();

        pipe.setParams(new SolvePNPPipeParams(coeffs, TargetModel.kAprilTag6p5in_36h11));
        var ret = pipe.run(List.of(new TrackedTarget(distortedCorners)));

        // rotate and try again
        var rotAngle = ImageRotationMode.DEG_90_CCW;
        var rotatedDistortedPoints =
                distortedCorners.stream().map(it -> rotAngle.rotatePoint(it, 1280, 720)).toList();
        pipe.setParams(
                new SolvePNPPipeParams(
                        coeffs.rotateCoefficients(rotAngle), TargetModel.kAprilTag6p5in_36h11));
        var retRotated = pipe.run(List.of(new TrackedTarget(rotatedDistortedPoints)));

        var pose_base = ret.output.get(0).getBestCameraToTarget3d();
        // So this is ostensibly a rotation about camera +Z,
        // but this is actually camera +X for our AprilTag pipe since we rotate to stay in ""WPILib""".
        // Negative to return to upright
        var pose_unrotated = retRotated.output.get(0).getBestCameraToTarget3d();

        System.out.println("Base: " + pose_base);
        System.out.println("rot-unrot: " + pose_unrotated);

        assertEquals(pose_base.getX(), pose_unrotated.getX(), 0.01);
        assertEquals(pose_base.getY(), pose_unrotated.getY(), 0.01);
        assertEquals(pose_base.getZ(), pose_unrotated.getZ(), 0.01);
        assertEquals(pose_base.getRotation().getX(), pose_unrotated.getRotation().getX(), 0.01);
        assertEquals(pose_base.getRotation().getY(), pose_unrotated.getRotation().getY(), 0.01);
        assertEquals(pose_base.getRotation().getZ(), pose_unrotated.getRotation().getZ(), 0.01);
    }
}

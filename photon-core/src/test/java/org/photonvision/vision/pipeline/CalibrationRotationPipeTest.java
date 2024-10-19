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

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
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
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;

public class CalibrationRotationPipeTest {
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

        // Use predefined camera calibration coefficients from TestUtils
        CameraCalibrationCoefficients coeffs = TestUtils.get2023LifeCamCoeffs(true);

        FrameStaticProperties frameProps =
                new FrameStaticProperties(
                        (int) coeffs.unrotatedImageSize.width,
                        (int) coeffs.unrotatedImageSize.height,
                        66,
                        coeffs);

        FrameStaticProperties rotatedFrameProps = frameProps.rotate(rot);

        Point[] originalPoints = {new Point(100, 100), new Point(200, 200), new Point(300, 100)};

        // Distort the origional points
        var distortedOriginalPoints =
                OpenCVHelp.distortPoints(
                        List.of(originalPoints),
                        frameProps.cameraCalibration.getCameraIntrinsicsMat(),
                        frameProps.cameraCalibration.getDistCoeffsMat());

        // and rotate them once distorted
        var rotatedDistortedPoints =
                distortedOriginalPoints.stream()
                        .map(it -> rot.rotatePoint(it, frameProps.imageWidth, frameProps.imageHeight))
                        .collect(Collectors.toList());

        // Now let's instead rotate then distort
        var rotatedOrigionalPoints =
                Arrays.stream(originalPoints)
                        .map(it -> rot.rotatePoint(it, frameProps.imageWidth, frameProps.imageHeight))
                        .collect(Collectors.toList());

        var distortedRotatedPoints =
                OpenCVHelp.distortPoints(
                        rotatedOrigionalPoints,
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
    public void testApriltagRotated() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag16h5;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag_corner_1280, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.getCoeffs(TestUtils.LIMELIGHT_480P_CAL_FILE, false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        frameProvider.requestFrameRotation(ImageRotationMode.DEG_0);
        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        var pose_base = pipelineResult.targets.get(0).getBestCameraToTarget3d();

        frameProvider.requestFrameRotation(ImageRotationMode.DEG_270_CCW);
        CVPipelineResult pipelineResult2 =
                pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        var pose_rotated = pipelineResult2.targets.get(0).getBestCameraToTarget3d();
        var pose_unrotated =
                new Transform3d(new Translation3d(), new Rotation3d(Units.degreesToRadians(270), 0, 0))
                        .plus(pose_rotated);

        Assertions.assertEquals(pose_base.getX(), pose_unrotated.getX(), 0.01);
        Assertions.assertEquals(pose_base.getY(), pose_unrotated.getY(), 0.01);
        Assertions.assertEquals(pose_base.getZ(), pose_unrotated.getZ(), 0.01);
        Assertions.assertEquals(
                pose_base.getRotation().getX(), pose_unrotated.getRotation().getX(), 0.01);
        Assertions.assertEquals(
                pose_base.getRotation().getY(), pose_unrotated.getRotation().getY(), 0.01);
        Assertions.assertEquals(
                pose_base.getRotation().getZ(), pose_unrotated.getRotation().getZ(), 0.01);
    }
}

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;

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

    @CartesianTest
    public void testUndistortImagePointsWithRotation(@Enum ImageRotationMode rot) {
        // Use predefined camera calibration coefficients from TestUtils
        CameraCalibrationCoefficients coeffs = TestUtils.get2023LifeCamCoeffs(true);

        FrameStaticProperties frameProps =
                new FrameStaticProperties(
                        (int) coeffs.unrotatedImageSize.width, (int) coeffs.unrotatedImageSize.height, -1, coeffs);
        FrameStaticProperties rotatedFrameProps = frameProps.rotate(rot);
        CameraCalibrationCoefficients rotatedCoeffs = rotatedFrameProps.cameraCalibration;

        Point[] originalPoints = {new Point(100, 100), new Point(200, 200)};
        MatOfPoint2f originalMatOfPoints = new MatOfPoint2f(originalPoints);

        MatOfPoint2f undistortedOriginalPoints = new MatOfPoint2f();
        Calib3d.undistortImagePoints(
                originalMatOfPoints,
                undistortedOriginalPoints,
                frameProps.cameraCalibration.getCameraIntrinsicsMat(),
                frameProps.cameraCalibration.getDistCoeffsMat());

        // Rotate the input points by 90 degrees
        Point[] rotatedInputPoints = new Point[originalPoints.length];
        for (int i = 0; i < originalPoints.length; i++) {
            rotatedInputPoints[i] =
                    new Point(rotatedFrameProps.imageWidth - originalPoints[i].y, originalPoints[i].x);
        }
        MatOfPoint2f rotatedMatOfPoints = new MatOfPoint2f(rotatedInputPoints);

        MatOfPoint2f undistortedRotatedPoints = new MatOfPoint2f();
        Calib3d.undistortImagePoints(
                rotatedMatOfPoints,
                undistortedRotatedPoints,
                rotatedCoeffs.getCameraIntrinsicsMat(),
                rotatedCoeffs.getDistCoeffsMat());

        // Rotate the undistorted original points by 90 degrees to get the expected rotated points
        Point[] undistortedOriginalArray = undistortedOriginalPoints.toArray();
        Point[] expectedRotatedPoints = new Point[undistortedOriginalArray.length];
        for (int i = 0; i < undistortedOriginalArray.length; i++) {
            expectedRotatedPoints[i] =
                    new Point(
                            rotatedFrameProps.imageWidth - undistortedOriginalArray[i].y,
                            undistortedOriginalArray[i].x);
        }
        Point[] rotatedPoints = undistortedRotatedPoints.toArray();

        assertArrayEquals(expectedRotatedPoints, rotatedPoints);
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

        frameProvider.requestFrameRotation(ImageRotationMode.DEG_90_CCW);
        CVPipelineResult pipelineResult2 = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        var pose_rotated = pipelineResult2.targets.get(0).getBestCameraToTarget3d();
        var pose_unrotated = new Transform3d(new Translation3d(), new Rotation3d(Units.degreesToRadians(180), 0, 0)).plus(pose_rotated);

        Assertions.assertEquals(pose_base.getX(), pose_unrotated.getX(), 0.01);
        Assertions.assertEquals(pose_base.getY(), pose_unrotated.getY(), 0.01);
        Assertions.assertEquals(pose_base.getZ(), pose_unrotated.getZ(), 0.01);
        Assertions.assertEquals(pose_base.getRotation().getX(), pose_unrotated.getRotation().getX(), 0.01);
        Assertions.assertEquals(pose_base.getRotation().getY(), pose_unrotated.getRotation().getY(), 0.01);
        Assertions.assertEquals(pose_base.getRotation().getZ(), pose_unrotated.getRotation().getZ(), 0.01);
    }
}

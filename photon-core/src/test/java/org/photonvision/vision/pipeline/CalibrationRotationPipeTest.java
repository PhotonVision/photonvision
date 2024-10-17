package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.ImageRotationMode;

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
    public void testUndistortImagePointsWithRotation() {
        // Use predefined camera calibration coefficients from TestUtils
        CameraCalibrationCoefficients coeffs = TestUtils.get2023LifeCamCoeffs(true);

        FrameStaticProperties frameProps =
                new FrameStaticProperties(
                        (int) coeffs.resolution.width, (int) coeffs.resolution.height, -1, coeffs);
        FrameStaticProperties rotatedFrameProps = frameProps.rotate(ImageRotationMode.DEG_270_CCW);
        CameraCalibrationCoefficients rotatedCoeffs = rotatedFrameProps.cameraCalibration;

        Point[] originalPoints = {new Point(100, 100), new Point(200, 200)};
        MatOfPoint2f originalMatOfPoints = new MatOfPoint2f(originalPoints);

        MatOfPoint2f undistortedOriginalPoints = new MatOfPoint2f();
        Calib3d.undistortPoints(
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
        Calib3d.undistortPoints(
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
}

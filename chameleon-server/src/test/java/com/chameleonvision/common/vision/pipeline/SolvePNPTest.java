package com.chameleonvision.common.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.util.TestUtils;
import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.provider.FileFrameProvider;
import com.chameleonvision.common.vision.opencv.CVMat;
import com.chameleonvision.common.vision.opencv.ContourGroupingMode;
import com.chameleonvision.common.vision.opencv.ContourIntersectionDirection;
import com.chameleonvision.common.vision.target.TargetModel;
import com.chameleonvision.common.vision.target.TrackedTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class SolvePNPTest {

    @Test
    public void meme() throws IOException {
        TestUtils.loadLibraries();

        var lowres = (Path.of(TestUtils.getCalibrationPath().toString(), "lifecamcal.json").toFile());
        var cal1 = new ObjectMapper().readValue(lowres, CameraCalibrationCoefficients.class);

        var highres = (Path.of(TestUtils.getCalibrationPath().toString(), "lifecamcal2.json").toFile());
        var cal2 = new ObjectMapper().readValue(highres, CameraCalibrationCoefficients.class);
    }

    private CameraCalibrationCoefficients get640p() {
        try {
            var cameraCalibration =
                    new ObjectMapper()
                            .readValue(
                                    (Path.of(TestUtils.getCalibrationPath().toString(), "lifecam640p.json").toFile()),
                                    CameraCalibrationCoefficients.class);

            assertEquals(3, cameraCalibration.cameraIntrinsics.rows);
            assertEquals(3, cameraCalibration.cameraIntrinsics.cols);
            assertEquals(1, cameraCalibration.cameraExtrinsics.rows);
            assertEquals(5, cameraCalibration.cameraExtrinsics.cols);
            assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMat().rows());
            assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMat().cols());
            assertEquals(1, cameraCalibration.cameraExtrinsics.getAsMat().rows());
            assertEquals(5, cameraCalibration.cameraExtrinsics.getAsMat().cols());
            assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMatOfDouble().rows());
            assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMatOfDouble().cols());
            assertEquals(1, cameraCalibration.cameraExtrinsics.getAsMatOfDouble().rows());
            assertEquals(5, cameraCalibration.cameraExtrinsics.getAsMatOfDouble().cols());
            assertEquals(3, cameraCalibration.getCameraIntrinsicsMat().rows());
            assertEquals(3, cameraCalibration.getCameraIntrinsicsMat().cols());
            assertEquals(1, cameraCalibration.getCameraExtrinsicsMat().rows());
            assertEquals(5, cameraCalibration.getCameraExtrinsicsMat().cols());

            return cameraCalibration;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void test2019() {
        TestUtils.loadLibraries();
        var pipeline = new ReflectivePipeline();

        var settings = new ReflectivePipelineSettings();
        settings.hsvHue.set(60, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(190, 255);
        settings.outputShowThresholded = true;
        settings.outputShowMultipleTargets = true;
        settings.solvePNPEnabled = true;
        settings.contourGroupingMode = ContourGroupingMode.Dual;
        settings.contourIntersection = ContourIntersectionDirection.Up;
        settings.cornerDetectionUseConvexHulls = true;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark48in),
                        TestUtils.WPI2019Image.FOV);

        CVPipelineResult pipelineResult;

        pipelineResult = pipeline.run(frameProvider.getFrame(), settings);

        TestUtils.showImage(pipelineResult.outputFrame.image.getMat(), "Pipeline output", 1000 * 90);
    }

    @Test
    public void test2020() {
        TestUtils.loadLibraries();
        var pipeline = new ReflectivePipeline();

        var settings = new ReflectivePipelineSettings();
        settings.hsvHue.set(60, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(60, 255);
        settings.outputShowThresholded = true;
        settings.solvePNPEnabled = true;
        settings.cornerDetectionAccuracyPercentage = 4;
        settings.cornerDetectionUseConvexHulls = true;
        settings.cameraCalibration = get640p();
        settings.targetModel = TargetModel.get2020Target(36);
        settings.cameraPitch = Rotation2d.fromDegrees(0.0);

        assertNotNull(settings.cameraCalibration);
        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.rows);
        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.cols);
        assertEquals(1, settings.cameraCalibration.cameraExtrinsics.rows);
        assertEquals(5, settings.cameraCalibration.cameraExtrinsics.cols);

        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.getAsMat().rows());
        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.getAsMat().cols());
        assertEquals(1, settings.cameraCalibration.cameraExtrinsics.getAsMat().rows());
        assertEquals(5, settings.cameraCalibration.cameraExtrinsics.getAsMat().cols());

        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.getAsMatOfDouble().rows());
        assertEquals(3, settings.cameraCalibration.cameraIntrinsics.getAsMatOfDouble().cols());
        assertEquals(1, settings.cameraCalibration.cameraExtrinsics.getAsMatOfDouble().rows());
        assertEquals(5, settings.cameraCalibration.cameraExtrinsics.getAsMatOfDouble().cols());

        assertEquals(3, settings.cameraCalibration.getCameraIntrinsicsMat().rows());
        assertEquals(3, settings.cameraCalibration.getCameraIntrinsicsMat().cols());
        assertEquals(1, settings.cameraCalibration.getCameraExtrinsicsMat().rows());
        assertEquals(5, settings.cameraCalibration.getCameraExtrinsicsMat().cols());

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_224in_Left),
                        TestUtils.WPI2020Image.FOV);

        //        TestUtils.showImage(frameProvider.getFrame().image.getMat(), "Pipeline output",
        // 999999);

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.getFrame(), settings);
        printTestResults(pipelineResult);

        var pose = pipelineResult.targets.get(0).getRobotRelativePose();
        //        assertEquals(180, pose.getTranslation().getX(), 20);
        //        assertEquals(0, pose.getTranslation().getY(), 20);
        //        assertEquals(0, pose.getRotation().getDegrees(), 5);

        TestUtils.showImage(pipelineResult.outputFrame.image.getMat(), "Pipeline output", 999999);
    }

    private static void continuouslyRunPipeline(Frame frame, ReflectivePipelineSettings settings) {
        var pipeline = new ReflectivePipeline();

        while (true) {
            CVPipelineResult pipelineResult = pipeline.run(frame, settings);
            printTestResults(pipelineResult);
            int preRelease = CVMat.getMatCount();
            pipelineResult.release();
            int postRelease = CVMat.getMatCount();

            System.out.printf("Pre: %d, Post: %d\n", preRelease, postRelease);
        }
    }

    // used to run VisualVM for profiling. It won't run on unit tests.
    public static void main(String[] args) {
        TestUtils.loadLibraries();
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes),
                        TestUtils.WPI2019Image.FOV);

        var settings = new ReflectivePipelineSettings();
        settings.hsvHue.set(60, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(190, 255);
        settings.outputShowThresholded = true;
        settings.outputShowMultipleTargets = true;
        settings.contourGroupingMode = ContourGroupingMode.Dual;
        settings.contourIntersection = ContourIntersectionDirection.Up;

        continuouslyRunPipeline(frameProvider.getFrame(), settings);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.println(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " " + "fps)");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
        System.out.println(
                "Found targets at "
                        + pipelineResult.targets.stream()
                                .map(TrackedTarget::getRobotRelativePose)
                                .collect(Collectors.toList()));
    }
}

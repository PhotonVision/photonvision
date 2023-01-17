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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class SolvePNPTest {
    private static final String LIFECAM_240P_CAL_FILE = "lifecam240p.json";
    private static final String LIFECAM_480P_CAL_FILE = "lifecam480p.json";

    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void loadCameraIntrinsics() {
        var lifecam240pCal = getCoeffs(LIFECAM_240P_CAL_FILE);
        var lifecam480pCal = getCoeffs(LIFECAM_480P_CAL_FILE);

        assertNotNull(lifecam240pCal);
        checkCameraCoefficients(lifecam240pCal);
        assertNotNull(lifecam480pCal);
        checkCameraCoefficients(lifecam480pCal);
    }

    private CameraCalibrationCoefficients getCoeffs(String filename) {
        var cameraCalibration = TestUtils.getCoeffs(filename, false);
        checkCameraCoefficients(cameraCalibration);
        return cameraCalibration;
    }

    private void checkCameraCoefficients(CameraCalibrationCoefficients cameraCalibration) {
        assertNotNull(cameraCalibration);
        assertEquals(3, cameraCalibration.cameraIntrinsics.rows);
        assertEquals(3, cameraCalibration.cameraIntrinsics.cols);
        assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMat().rows());
        assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMat().cols());
        assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMatOfDouble().rows());
        assertEquals(3, cameraCalibration.cameraIntrinsics.getAsMatOfDouble().cols());
        assertEquals(3, cameraCalibration.getCameraIntrinsicsMat().rows());
        assertEquals(3, cameraCalibration.getCameraIntrinsicsMat().cols());
        assertEquals(1, cameraCalibration.distCoeffs.rows);
        assertEquals(5, cameraCalibration.distCoeffs.cols);
        assertEquals(1, cameraCalibration.distCoeffs.getAsMat().rows());
        assertEquals(5, cameraCalibration.distCoeffs.getAsMat().cols());
        assertEquals(1, cameraCalibration.distCoeffs.getAsMatOfDouble().rows());
        assertEquals(5, cameraCalibration.distCoeffs.getAsMatOfDouble().cols());
        assertEquals(1, cameraCalibration.getDistCoeffsMat().rows());
        assertEquals(5, cameraCalibration.getDistCoeffsMat().cols());
    }

    @Test
    public void test2019() {
        var pipeline = new ReflectivePipeline();

        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputShowMultipleTargets = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Dual;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.k2019DualTarget;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark48in, false),
                        TestUtils.WPI2019Image.FOV,
                        TestUtils.get2019LifeCamCoeffs(false));

        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());
        var hsvParams =
                new HSVPipe.HSVParams(
                        pipeline.getSettings().hsvHue,
                        pipeline.getSettings().hsvSaturation,
                        pipeline.getSettings().hsvValue,
                        pipeline.getSettings().hueInverted);
        frameProvider.requestHsvSettings(hsvParams);

        CVPipelineResult pipelineResult;

        pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

        // these numbers are not *accurate*, but they are known and expected
        var pose = pipelineResult.targets.get(0).getBestCameraToTarget3d();
        Assertions.assertEquals(1.1, pose.getTranslation().getX(), 0.05);
        Assertions.assertEquals(0.0, pose.getTranslation().getY(), 0.05);

        // We expect the object X to be forward, or -X in world space
        Assertions.assertEquals(
                -1, new Translation3d(1, 0, 0).rotateBy(pose.getRotation()).getX(), 0.05);
        // We expect the object Y axis to be right, or negative-Y in world space
        Assertions.assertEquals(
                -1, new Translation3d(0, 1, 0).rotateBy(pose.getRotation()).getY(), 0.05);
        // We expect the object Z axis to be up, or +Z in world space
        Assertions.assertEquals(
                1, new Translation3d(0, 0, 1).rotateBy(pose.getRotation()).getZ(), 0.05);

        TestUtils.showImage(
                pipelineResult.inputAndOutputFrame.colorImage.getMat(), "Pipeline output", 999999);
    }

    @Test
    public void test2020() {
        var pipeline = new ReflectivePipeline();

        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(60, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.k2020HighGoalOuter;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_224in_Left, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));

        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());
        var hsvParams =
                new HSVPipe.HSVParams(
                        pipeline.getSettings().hsvHue,
                        pipeline.getSettings().hsvSaturation,
                        pipeline.getSettings().hsvValue,
                        pipeline.getSettings().hueInverted);
        frameProvider.requestHsvSettings(hsvParams);

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

        // Draw on input
        var outputPipe = new OutputStreamPipeline();
        outputPipe.process(
                pipelineResult.inputAndOutputFrame, pipeline.getSettings(), pipelineResult.targets);

        // these numbers are not *accurate*, but they are known and expected
        var pose = pipelineResult.targets.get(0).getBestCameraToTarget3d();
        Assertions.assertEquals(Units.inchesToMeters(240.26), pose.getTranslation().getX(), 0.05);
        Assertions.assertEquals(Units.inchesToMeters(35), pose.getTranslation().getY(), 0.05);
        // Z rotation should be mostly facing us
        Assertions.assertEquals(Units.degreesToRadians(-140), pose.getRotation().getZ(), 1);

        TestUtils.showImage(
                pipelineResult.inputAndOutputFrame.colorImage.getMat(), "Pipeline output", 999999);
    }

    private static void continuouslyRunPipeline(Frame frame, ReflectivePipelineSettings settings) {
        var pipeline = new ReflectivePipeline();
        pipeline.settings = settings;

        while (true) {
            CVPipelineResult pipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
            printTestResults(pipelineResult);
            int preRelease = CVMat.getMatCount();
            pipelineResult.release();
            int postRelease = CVMat.getMatCount();

            System.out.printf("Pre: %d, Post: %d\n", preRelease, postRelease);
        }
    }

    // used to run VisualVM for profiling, which won't run on unit tests.
    public static void main(String[] args) {
        TestUtils.loadLibraries();
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        var settings = new ReflectivePipelineSettings();
        settings.hsvHue.set(60, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(190, 255);
        settings.outputShouldDraw = true;
        settings.outputShowMultipleTargets = true;
        settings.contourGroupingMode = ContourGroupingMode.Dual;
        settings.contourIntersection = ContourIntersectionDirection.Up;

        continuouslyRunPipeline(frameProvider.get(), settings);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.println(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " " + "fps)");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
        System.out.println(
                "Found targets at "
                        + pipelineResult.targets.stream()
                                .map(TrackedTarget::getBestCameraToTarget3d)
                                .collect(Collectors.toList()));
    }
}

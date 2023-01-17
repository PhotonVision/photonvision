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

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Collectors;
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
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class CirclePNPTest {
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
    public void testCircle() {
        var pipeline = new ColoredShapePipeline();

        pipeline.getSettings().hsvHue.set(0, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(100, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().maxCannyThresh = 50;
        pipeline.getSettings().circleAccuracy = 15;
        pipeline.getSettings().circleDetectThreshold = 5;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().cameraCalibration = getCoeffs(LIFECAM_480P_CAL_FILE);
        pipeline.getSettings().targetModel = TargetModel.kCircularPowerCell7in;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputShowMultipleTargets = false;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Single;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().contourShape = ContourShape.Circle;
        pipeline.getSettings().circleDetectThreshold = 10;
        pipeline.getSettings().contourRadius.setFirst(30);
        pipeline.getSettings().accuracyPercentage = 30.0;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getPowercellImagePath(TestUtils.PowercellTestImages.kPowercell_test_6, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(true));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

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

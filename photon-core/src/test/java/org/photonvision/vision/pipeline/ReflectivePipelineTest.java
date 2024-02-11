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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class ReflectivePipelineTest {
    @Test
    public void test2019() {
        TestUtils.loadLibraries();
        var pipeline = new ReflectivePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputShowMultipleTargets = true;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Dual;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());
        var hsvParams =
                new HSVPipe.HSVParams(
                        pipeline.getSettings().hsvHue,
                        pipeline.getSettings().hsvSaturation,
                        pipeline.getSettings().hsvValue,
                        pipeline.getSettings().hueInverted);
        frameProvider.requestHsvSettings(hsvParams);

        TestUtils.showImage(frameProvider.get().colorImage.getMat(), "Pipeline input", 1);

        CVPipelineResult pipelineResult;

        pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

        Assertions.assertTrue(pipelineResult.hasTargets());
        Assertions.assertEquals(2, pipelineResult.targets.size(), "Target count wrong!");

        TestUtils.showImage(pipelineResult.inputAndOutputFrame.colorImage.getMat(), "Pipeline output");
    }

    @Test
    public void test2020() {
        TestUtils.loadLibraries();
        var pipeline = new ReflectivePipeline();

        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(200, 255);
        pipeline.getSettings().hsvValue.set(200, 255);
        pipeline.getSettings().outputShouldDraw = true;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_108in_Center, false),
                        TestUtils.WPI2020Image.FOV);

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

        TestUtils.showImage(
                pipelineResult.inputAndOutputFrame.processedImage.getMat(), "Pipeline output");
    }

    private static void continuouslyRunPipeline(Frame frame, ReflectivePipelineSettings settings) {
        var pipeline = new ReflectivePipeline();

        while (true) {
            CVPipelineResult pipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
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
        System.out.print(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " fps), ");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
    }
}

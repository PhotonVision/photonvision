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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class UncropColoredShapeTest {
    @BeforeEach
    public void setup() {
        TestUtils.loadLibraries();
        ConfigManager.getInstance().load();
    }

    @Test
    public void testReflective() {
        var pipeline = new ReflectivePipeline();

        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(200, 255);
        pipeline.getSettings().hsvValue.set(200, 255);
        pipeline.getSettings().outputShouldDraw = true;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_060in_Center, false),
                        TestUtils.WPI2020Image.FOV);

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        printTestResults(pipelineResult);

        TestUtils.showImage(
                pipelineResult.inputAndOutputFrame.processedImage.getMat(), "Pipeline output");

        pipeline.getSettings().static_x = 100;

        CVPipelineResult croppedResults;
        croppedResults = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        printTestResults(croppedResults);
        var outputPipe = new OutputStreamPipeline();

        var ret_cropped =
                outputPipe.process(
                        croppedResults.inputAndOutputFrame, pipeline.getSettings(), croppedResults.targets);

        TestUtils.showImage(
                ret_cropped.inputAndOutputFrame.processedImage.getMat(), "Cropped Pipeline output", 999999);
        // TrackedTarget target = pipelineResult.targets.get(0);
        // TrackedTarget cropped = croppedResults.targets.get(0);

        // Assertions.assertEquals(target.getTargetCorners().get(0).x,
        // cropped.getTargetCorners().get(0).x, 1);

    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.print(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " fps), ");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
    }
}

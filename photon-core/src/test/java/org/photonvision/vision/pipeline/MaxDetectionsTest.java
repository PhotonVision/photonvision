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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class MaxDetectionsTest {
    @Test
    public void testMaxDetections() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();

        ColoredShapePipeline pipeline = new ColoredShapePipeline();

        pipeline.settings.contourShape = ContourShape.Circle;
        pipeline.settings.hsvHue.set(140, 160);
        pipeline.settings.hsvSaturation.set(226, 246);
        pipeline.settings.hsvValue.set(188, 208);
        pipeline.settings.maxCannyThresh = 90;
        pipeline.settings.circleAccuracy = 20;
        pipeline.settings.circleDetectThreshold = 5;

        Path path =
                TestUtils.getResourcesFolderPath(false).resolve("testimages/polygons/ColoredShapeTest.png");

        var frameProvider = new FileFrameProvider(path, TestUtils.WPI2019Image.FOV);

        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        TestUtils.showImage(result.inputAndOutputFrame.processedImage.getMat(), "Max Detections Test");

        assertEquals(20, result.targets.size());

        pipeline.settings.outputMaximumTargets = 5;
        result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertEquals(5, result.targets.size());

        pipeline.settings.outputMaximumTargets = 50;
        result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertEquals(24, result.targets.size());
    }
}

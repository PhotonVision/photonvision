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

import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class MaxDetectionsTest {
    @Test
    public void testMaxDetections() {
        ColoredShapePipeline pipeline = new ColoredShapePipeline();

        pipeline.settings.contourShape = ContourShape.Circle;
        pipeline.settings.hsvHue.set(10, 40);
        pipeline.settings.hsvSaturation.set(100, 255);
        pipeline.settings.hsvValue.set(100, 255);
        pipeline.settings.maxCannyThresh = 50;
        pipeline.settings.circleAccuracy = 15;
        pipeline.settings.circleDetectThreshold = 5;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getResourcesFolderPath(false)
                                .resolve("testimages/polygons/ColoredShapeTest.png"),
                        TestUtils.WPI2019Image.FOV);
        CVPipelineResult result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        assertEquals(20, result.targets.size());

        pipeline.settings.outputMaximumTargets = 5;
        result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertEquals(5, result.targets.size());

        pipeline.settings.outputMaximumTargets = 50;
        result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertEquals(24, result.targets.size());
    }
}

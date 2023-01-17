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

import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class ColoredShapePipelineTest {
    public static void testTriangleDetection(
            ColoredShapePipeline pipeline, ColoredShapePipelineSettings settings, Frame frame) {
        pipeline.settings = settings;
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
        TestUtils.showImage(
                colouredShapePipelineResult.inputAndOutputFrame.processedImage.getMat(),
                "Pipeline output: Triangle.");
        printTestResults(colouredShapePipelineResult);
    }

    public static void testQuadrilateralDetection(
            ColoredShapePipeline pipeline, ColoredShapePipelineSettings settings, Frame frame) {
        settings.contourShape = ContourShape.Quadrilateral;
        pipeline.settings = settings;
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
        TestUtils.showImage(
                colouredShapePipelineResult.inputAndOutputFrame.processedImage.getMat(),
                "Pipeline output: Quadrilateral.");
        printTestResults(colouredShapePipelineResult);
    }

    public static void testCustomShapeDetection(
            ColoredShapePipeline pipeline, ColoredShapePipelineSettings settings, Frame frame) {
        settings.contourShape = ContourShape.Custom;
        pipeline.settings = settings;
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
        TestUtils.showImage(
                colouredShapePipelineResult.inputAndOutputFrame.processedImage.getMat(),
                "Pipeline output: Custom.");
        printTestResults(colouredShapePipelineResult);
    }

    @Test
    public static void testCircleShapeDetection(
            ColoredShapePipeline pipeline, ColoredShapePipelineSettings settings, Frame frame) {
        settings.contourShape = ContourShape.Circle;
        pipeline.settings = settings;
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame, QuirkyCamera.DefaultCamera);
        TestUtils.showImage(
                colouredShapePipelineResult.inputAndOutputFrame.processedImage.getMat(),
                "Pipeline output: Circle.");
        printTestResults(colouredShapePipelineResult);
    }

    @Test
    public static void testPowercellDetection(
            ColoredShapePipelineSettings settings, ColoredShapePipeline pipeline) {
        settings.hsvHue.set(10, 40);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(100, 255);
        settings.maxCannyThresh = 50;
        settings.circleAccuracy = 15;
        settings.circleDetectThreshold = 5;
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getPowercellImagePath(TestUtils.PowercellTestImages.kPowercell_test_6, false),
                        TestUtils.WPI2019Image.FOV);
        testCircleShapeDetection(pipeline, settings, frameProvider.get());
    }

    public static void main(String[] args) {
        TestUtils.loadLibraries();
        System.out.println(
                TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_108in_Center, false));
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getPolygonImagePath(TestUtils.PolygonTestImages.kPolygons, false),
                        TestUtils.WPI2019Image.FOV);
        var settings = new ColoredShapePipelineSettings();
        settings.hsvHue.set(0, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(100, 255);
        settings.outputShouldDraw = true;
        settings.outputShowMultipleTargets = true;
        settings.contourGroupingMode = ContourGroupingMode.Single;
        settings.contourIntersection = ContourIntersectionDirection.Up;
        settings.contourShape = ContourShape.Triangle;
        settings.circleDetectThreshold = 10;
        settings.accuracyPercentage = 30.0;

        ColoredShapePipeline pipeline = new ColoredShapePipeline();
        testTriangleDetection(pipeline, settings, frameProvider.get());
        testQuadrilateralDetection(pipeline, settings, frameProvider.get());
        testCustomShapeDetection(pipeline, settings, frameProvider.get());
        //        testCircleShapeDetection(pipeline, settings, frameProvider.get());
        //        testPowercellDetection(settings, pipeline);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.print(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " fps), ");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
    }
}

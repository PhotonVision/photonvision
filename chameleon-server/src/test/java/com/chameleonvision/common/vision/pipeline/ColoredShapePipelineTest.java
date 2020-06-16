package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.util.TestUtils;
import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;
import com.chameleonvision.common.vision.frame.provider.FileFrameProvider;
import com.chameleonvision.common.vision.opencv.ContourGroupingMode;
import com.chameleonvision.common.vision.opencv.ContourIntersectionDirection;
import com.chameleonvision.common.vision.opencv.ContourShape;
import org.junit.jupiter.api.Test;

public class ColoredShapePipelineTest {

    public static void testTriangleDetection(
            ColoredShapePipeline pipeline,
            ColoredShapePipelineSettings settings,
            FrameStaticProperties frameStaticProperties,
            Frame frame) {
        pipeline.setPipeParams(frameStaticProperties, settings);
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame);
        TestUtils.showImage(
                colouredShapePipelineResult.outputFrame.image.getMat(), "Pipeline output: Triangle.");
        printTestResults(colouredShapePipelineResult);
    }

    public static void testQuadrilateralDetection(
            ColoredShapePipeline pipeline,
            ColoredShapePipelineSettings settings,
            FrameStaticProperties frameStaticProperties,
            Frame frame) {
        settings.desiredShape = ContourShape.Quadrilateral;
        pipeline.setPipeParams(frameStaticProperties, settings);
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame);
        TestUtils.showImage(
                colouredShapePipelineResult.outputFrame.image.getMat(), "Pipeline output: Quadrilateral.");
        printTestResults(colouredShapePipelineResult);
    }

    public static void testCustomShapeDetection(
            ColoredShapePipeline pipeline,
            ColoredShapePipelineSettings settings,
            FrameStaticProperties frameStaticProperties,
            Frame frame) {
        settings.desiredShape = ContourShape.Custom;
        pipeline.setPipeParams(frameStaticProperties, settings);
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame);
        TestUtils.showImage(
                colouredShapePipelineResult.outputFrame.image.getMat(), "Pipeline output: Custom.");
        printTestResults(colouredShapePipelineResult);
    }

    @Test
    public static void testCircleShapeDetection(
            ColoredShapePipeline pipeline,
            ColoredShapePipelineSettings settings,
            FrameStaticProperties frameStaticProperties,
            Frame frame) {
        settings.desiredShape = ContourShape.Circle;
        pipeline.setPipeParams(frameStaticProperties, settings);
        CVPipelineResult colouredShapePipelineResult = pipeline.run(frame);
        TestUtils.showImage(
                colouredShapePipelineResult.outputFrame.image.getMat(), "Pipeline output: Circle.");
        printTestResults(colouredShapePipelineResult);
    }

    @Test
    public static void testPowercellDetection(
            ColoredShapePipelineSettings settings, ColoredShapePipeline pipeline) {

        settings.hsvHue.set(10, 40);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(100, 255);
        settings.maxCannyThresh = 50;
        settings.accuracy = 15;
        settings.allowableThreshold = 5;
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getPowercellImagePath(TestUtils.PowercellTestImages.kPowercell_test_6),
                        TestUtils.WPI2019Image.FOV);
        testCircleShapeDetection(
                pipeline, settings, frameProvider.get().frameStaticProperties, frameProvider.get());
    }

    public static void main(String[] args) {
        TestUtils.loadLibraries();
        System.out.println(TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_108in_Center));
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getPolygonImagePath(TestUtils.PolygonTestImages.kPolygons),
                        TestUtils.WPI2019Image.FOV);
        var settings = new ColoredShapePipelineSettings();
        settings.hsvHue.set(0, 100);
        settings.hsvSaturation.set(100, 255);
        settings.hsvValue.set(100, 255);
        settings.outputShowThresholded = true;
        settings.outputShowMultipleTargets = true;
        settings.contourGroupingMode = ContourGroupingMode.Single;
        settings.contourIntersection = ContourIntersectionDirection.Up;
        settings.desiredShape = ContourShape.Triangle;
        settings.allowableThreshold = 10;
        settings.accuracyPercentage = 30.0;

        ColoredShapePipeline pipeline = new ColoredShapePipeline();
        testTriangleDetection(
                pipeline, settings, frameProvider.get().frameStaticProperties, frameProvider.get());
        testQuadrilateralDetection(
                pipeline, settings, frameProvider.get().frameStaticProperties, frameProvider.get());
        testCustomShapeDetection(
                pipeline, settings, frameProvider.get().frameStaticProperties, frameProvider.get());
        testCircleShapeDetection(
                pipeline, settings, frameProvider.get().frameStaticProperties, frameProvider.get());
        testPowercellDetection(settings, pipeline);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.print(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " fps), ");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
    }
}

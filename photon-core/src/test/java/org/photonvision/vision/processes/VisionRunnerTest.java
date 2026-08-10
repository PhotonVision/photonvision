package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.ArucoPipeline;

public class VisionRunnerTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    // TODO consider some sort of test fixture
    private static class RecordingFrameProvider extends FrameProvider {
        boolean copyInput;
        boolean copyOutput;

        @Override
        protected boolean checkCameraConnected() {
            return true;
        }

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public void requestFrameThresholdType(FrameThresholdType type) {}

        @Override
        public void requestFrameRotation(ImageRotationMode rotationMode) {}

        @Override
        public void requestFrameCopies(boolean copyInput, boolean copyOutput) {
            this.copyInput = copyInput;
            this.copyOutput = copyOutput;
        }

        @Override
        public void requestHsvSettings(HSVPipe.HSVParams params) {}

        @Override
        public void requestBlockForFrames(boolean blockForFrames) {}

        @Override
        public Frame get() {
            return new Frame();
        }

        @Override
        public void release() {}
    }

    @CartesianTest
    public void testThresholdRequestsAruco(
            @Values(booleans = {true, false}) boolean debugThreshold,
            @Values(booleans = {true, false}) boolean inputShouldShow,
            @Values(booleans = {true, false}) boolean outputShouldShow) {
        try (var pipeline = new ArucoPipeline()) {
            // given an Aruco pipeline with settings for debugThreshold, inputShouldShow,
            // and outputShouldShow
            var provider = new RecordingFrameProvider();
            pipeline.getSettings().debugThreshold = debugThreshold;
            pipeline.getSettings().inputShouldShow = inputShouldShow;
            pipeline.getSettings().outputShouldShow = outputShouldShow;

            // When we configure the frame provider based on pipeline settings
            VisionRunner.configureFrameProviderForPipeline(provider, pipeline);

            // Then the input color image is copied when expected
            var expectedCopyInput = inputShouldShow || debugThreshold;
            // And output is copied whn needed
            boolean expectedCopyOutput = outputShouldShow;

            assertEquals(expectedCopyInput, provider.copyInput);
            assertEquals(expectedCopyOutput, provider.copyOutput);
        }
    }
}

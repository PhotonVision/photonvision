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

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.*;

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

    /**
     * Pipelines under test, with needsColor/needsProcessed hardcoded from whether {@code process}
     * reads {@code frame.colorImage} or {@code frame.processedImage} as input.
     *
     * <p>Aruco's {@code debugThreshold} color-copy behavior is covered separately.
     */
    @SuppressWarnings("rawtypes")
    private enum PipelineUnderTest {
        APRILTAG(AprilTagPipeline::new, false, true),

        ARUCO(ArucoPipeline::new, false, true),
        // When Aruco is used with debug threshold, we need to copy the color image
        ARUCO_DEBUG(ArucoPipeline::new, true, true),

        REFLECTIVE(ReflectivePipeline::new, false, true),
        COLORED_SHAPE(ColoredShapePipeline::new, false, true),
        OBJECT_DETECTION(ObjectDetectionPipeline::new, true, false),
        FOCUS(FocusPipeline::new, true, false),
        DRIVER_MODE(DriverModePipeline::new, true, false),
        CALIBRATE_3D(Calibrate3dPipeline::new, true, false);

        private final Supplier<? extends CVPipeline> factory;
        private final boolean needsColor;
        private final boolean needsProcessed;

        PipelineUnderTest(
                Supplier<? extends CVPipeline> factory, boolean needsColor, boolean needsProcessed) {
            this.factory = factory;
            this.needsColor = needsColor;
            this.needsProcessed = needsProcessed;
        }

        CVPipeline create() {
            return factory.get();
        }
    }

    @CartesianTest
    public void testFrameCopyRequests(
            @Enum PipelineUnderTest pipelineUnderTest,
            @Values(booleans = {true, false}) boolean inputShouldShow,
            @Values(booleans = {true, false}) boolean outputShouldShow) {
        try (var pipeline = pipelineUnderTest.create()) {
            var provider = new RecordingFrameProvider();
            pipeline.getSettings().inputShouldShow = inputShouldShow;
            pipeline.getSettings().outputShouldShow = outputShouldShow;

            // Aruco with debug threshold is a special case
            if (pipelineUnderTest == PipelineUnderTest.ARUCO_DEBUG) {
                ArucoPipelineSettings arucoPipelineSettings =
                        (ArucoPipelineSettings) pipeline.getSettings();

                arucoPipelineSettings.debugThreshold = true;
            }

            VisionRunner.configureFrameProviderForPipeline(provider, pipeline);

            boolean expectedCopyInput = pipelineUnderTest.needsColor || inputShouldShow;
            boolean expectedCopyOutput = pipelineUnderTest.needsProcessed || outputShouldShow;

            assertEquals(expectedCopyInput, provider.copyInput);
            assertEquals(expectedCopyOutput, provider.copyOutput);
        }
    }
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

class GrayscaleCaptureRequestTest {
    @BeforeAll
    static void loadOpenCv() {
        assertTrue(LibraryLoader.loadWpiLibraries());
    }

    @Test
    void grayscaleRequiresOptInAndResetsOnPipelineSwitch() {
        var property = "photonvision.grayscaleAprilTagCapture";
        var previousValue = System.getProperty(property);
        var provider = new RecordingProvider();
        try (var aprilTag = new StubPipeline(new AprilTagPipelineSettings());
                var reflective = new StubPipeline(new ReflectivePipelineSettings())) {
            System.clearProperty(property);
            VisionRunner.configureFrameProviderForPipeline(provider, aprilTag);
            assertFalse(provider.grayscale);

            System.setProperty(property, "true");
            VisionRunner.configureFrameProviderForPipeline(provider, aprilTag);
            assertTrue(provider.grayscale);

            VisionRunner.configureFrameProviderForPipeline(provider, reflective);
            assertFalse(provider.grayscale);

            System.setProperty(property, "false");
            VisionRunner.configureFrameProviderForPipeline(provider, aprilTag);
            assertFalse(provider.grayscale);
        } finally {
            if (previousValue == null) System.clearProperty(property);
            else System.setProperty(property, previousValue);
        }
    }

    private static class StubPipeline extends CVPipeline<CVPipelineResult, CVPipelineSettings> {
        StubPipeline(CVPipelineSettings settings) {
            super(FrameThresholdType.GREYSCALE);
            this.settings = settings;
        }

        @Override
        protected void setPipeParamsImpl() {}

        @Override
        protected CVPipelineResult process(Frame frame, CVPipelineSettings settings) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingProvider extends FrameProvider {
        boolean grayscale;

        @Override
        public void requestGrayscaleInput(boolean grayscaleInput) {
            grayscale = grayscaleInput;
        }

        @Override
        public void requestFrameThresholdType(FrameThresholdType type) {}

        @Override
        public void requestFrameRotation(ImageRotationMode rotation) {}

        @Override
        public void requestFrameCopies(boolean input, boolean output) {}

        @Override
        public void requestHsvSettings(HSVPipe.HSVParams params) {}

        @Override
        public void requestBlockForFrames(boolean block) {}

        @Override
        protected boolean checkCameraConnected() {
            return true;
        }

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public Frame get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void release() {}
    }
}

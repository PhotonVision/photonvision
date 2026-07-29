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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.frame.provider.FileLogFrameProvider;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe.HSVParams;

/**
 * Exercises {@link VisionSource#setFrameProvider}, {@link VisionSource#getFrameProvider}, and
 * {@link VisionSource#getReplayRecordingDir}. The contract under test: after a swap, callers that
 * read through the canonical getter observe the new provider on the very next call. That's what
 * lets {@code VisionRunner} pick up an in-place replay swap without a runner restart.
 */
class VisionSourceFrameProviderSwapTest {
    @BeforeAll
    static void loadOpenCv() {
        if (!LibraryLoader.loadWpiLibraries()) {
            fail("Failed to load WPI / OpenCV native libraries");
        }
    }

    private static CameraConfiguration newConfig() {
        return new CameraConfiguration(PVCameraInfo.fromFileInfo("/dev/null", "swap-test"));
    }

    @Test
    void getterReturnsTheProviderRegisteredViaSetter() {
        var source = new TestVisionSource(newConfig());
        var first = new NamedNoopProvider("first");
        source.setFrameProvider(first);
        assertSame(first, source.getFrameProvider());
    }

    @Test
    void swapIsImmediatelyVisibleThroughTheGetter() {
        var source = new TestVisionSource(newConfig());
        var live = new NamedNoopProvider("live");
        var replay = new NamedNoopProvider("replay");
        source.setFrameProvider(live);
        assertSame(live, source.getFrameProvider());

        source.setFrameProvider(replay);
        assertSame(replay, source.getFrameProvider(), "post-swap read must see the new provider");
    }

    @Test
    void replayRecordingDirIsEmptyForNonFileLogProviders() {
        var source = new TestVisionSource(newConfig());
        source.setFrameProvider(new NamedNoopProvider("live"));
        assertTrue(source.getReplayRecordingDir().isEmpty());
    }

    @Test
    void replayRecordingDirIsEmptyWhenNoProviderRegistered() {
        var source = new TestVisionSource(newConfig());
        assertNull(source.getFrameProvider());
        assertTrue(source.getReplayRecordingDir().isEmpty());
    }

    @Test
    void replayRecordingDirReturnsTheFileLogProvidersDir(@TempDir Path tmp) throws IOException {
        Path recording = createMinimalRecording(tmp);
        var fileLog = new FileLogFrameProvider(recording);
        try {
            var source = new TestVisionSource(newConfig());
            source.setFrameProvider(fileLog);

            var dir = source.getReplayRecordingDir();
            assertTrue(dir.isPresent(), "FileLog provider must surface a recording dir");
            assertEquals(recording.toAbsolutePath(), dir.get().toAbsolutePath());
        } finally {
            fileLog.release();
        }
    }

    @Test
    void replayRecordingDirGoesEmptyAfterSwapToLiveProvider(@TempDir Path tmp) throws IOException {
        Path recording = createMinimalRecording(tmp);
        var fileLog = new FileLogFrameProvider(recording);
        try {
            var source = new TestVisionSource(newConfig());
            source.setFrameProvider(fileLog);
            assertTrue(source.getReplayRecordingDir().isPresent());

            source.setFrameProvider(new NamedNoopProvider("post-replay"));
            assertFalse(source.getReplayRecordingDir().isPresent());
        } finally {
            fileLog.release();
        }
    }

    /**
     * Writes a one-frame recording (frames/000000.jpg + metadata.jsonl) that satisfies the
     * FileLogFrameProvider constructor.
     */
    private static Path createMinimalRecording(Path tmp) throws IOException {
        Path recording = tmp.resolve("recording");
        Path frames = recording.resolve("frames");
        Files.createDirectories(frames);

        Mat img = new Mat(8, 8, CvType.CV_8UC3, new Scalar(0, 0, 0));
        try {
            Imgcodecs.imwrite(frames.resolve("000000.jpg").toString(), img);
        } finally {
            img.release();
        }

        Path metadata = recording.resolve("metadata.jsonl");
        try (BufferedWriter w = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
            w.write("{\"seq\":0,\"capture_ns\":1000000000}");
            w.newLine();
        }
        return recording;
    }

    private static final class TestVisionSource extends VisionSource {
        TestVisionSource(CameraConfiguration config) {
            super(config);
        }

        @Override
        public VisionSourceSettables getSettables() {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public boolean isVendorCamera() {
            return false;
        }

        @Override
        public boolean hasLEDs() {
            return false;
        }

        @Override
        public void remakeSettables() {}

        @Override
        public void release() {}
    }

    private static final class NamedNoopProvider extends FrameProvider {
        private final String name;

        NamedNoopProvider(String name) {
            this.name = name;
        }

        @Override
        public Frame get() {
            return new Frame();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        protected boolean checkCameraConnected() {
            return true;
        }

        @Override
        public void requestFrameThresholdType(FrameThresholdType type) {}

        @Override
        public void requestFrameRotation(ImageRotationMode rotationMode) {}

        @Override
        public void requestFrameCopies(boolean copyInput, boolean copyOutput) {}

        @Override
        public void requestHsvSettings(HSVParams params) {}

        @Override
        public void requestBlockForFrames(boolean blockForFrames) {}

        @Override
        public void release() {}
    }
}

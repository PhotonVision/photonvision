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

package org.photonvision.vision.frame.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.photonvision.jni.LibraryLoader;

/**
 * Integration-ish test for {@link FileLogFrameProvider}. Synthesises a tiny image-sequence
 * fixture (3 JPEGs in a frames/ directory) with OpenCV's own {@link Imgcodecs#imwrite} — the
 * same encoder path {@code FrameRecorder} uses in production — pairs it with a hand-written
 * sidecar, then walks it through the provider and asserts frame count, dimensions, channel
 * order, and timestamp propagation.
 *
 * <p>Requires the OpenCV native libraries (loaded in {@code @BeforeAll} via
 * {@link LibraryLoader#loadWpiLibraries()}). Pure JPEG I/O — works on every platform PV builds
 * on with no external codec deps.
 *
 * <p>Notably this test does <em>not</em> call {@link LibraryLoader#loadTargeting()} or initialise
 * HAL/NetworkTables — {@code FileLogFrameProvider} touches no PV-specific JNI singletons, so it
 * runs on platforms where {@code photontargetingJNI} is unavailable (e.g. a Windows dev box
 * without Visual Studio).
 */
class FileLogFrameProviderTest {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 48;
    private static final long[] CAPTURE_NS = {1_000_000_000L, 1_033_333_333L, 1_066_666_666L};

    @TempDir static Path sharedTempDir;
    static Path recordingDir;

    @BeforeAll
    static void setUp() throws IOException {
        if (!LibraryLoader.loadWpiLibraries()) {
            fail("Failed to load WPI / OpenCV native libraries");
        }

        recordingDir = sharedTempDir.resolve("rec");
        Files.createDirectories(recordingDir);

        synthesizeFrames(recordingDir.resolve("frames"));
        writeSidecar(recordingDir.resolve("metadata.jsonl"));
    }

    /**
     * Generate a deterministic 3-frame image-sequence the same way {@code FrameRecorder} does in
     * production — OpenCV {@code Imgcodecs.imwrite} of zero-padded JPEGs. Each frame is a solid
     * BGR colour that shifts so the JPEG encoder sees real per-frame variation.
     */
    private static void synthesizeFrames(Path framesDir) throws IOException {
        Files.createDirectories(framesDir);
        for (int i = 0; i < CAPTURE_NS.length; i++) {
            Mat frame = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3, new Scalar(i * 64 % 255, 64, 128));
            try {
                Path out = framesDir.resolve(String.format("%06d.jpg", i));
                assertTrue(
                        Imgcodecs.imwrite(out.toString(), frame),
                        "Imgcodecs.imwrite failed for " + out);
            } finally {
                frame.release();
            }
        }
    }

    private static void writeSidecar(Path out) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                w.write("{\"seq\":" + i + ",\"capture_ns\":" + CAPTURE_NS[i] + "}\n");
            }
        }
    }

    @Test
    void emitsFramesWithCaptureTimestamps() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertTrue(provider.isConnected(), "should be connected before any frames emitted");
            assertTrue(
                    provider.hasConnected(),
                    "isConnected() should have flipped cameraPropertiesCached on first call");

            for (int i = 0; i < CAPTURE_NS.length; i++) {
                var captured = provider.getInputMat();
                assertNotNull(captured.colorImage, "frame " + i + ": colorImage null");
                assertFalse(
                        captured.colorImage.getMat().empty(),
                        "frame " + i + ": Mat was empty (decoder failed?)");
                assertEquals(
                        WIDTH,
                        captured.colorImage.getMat().cols(),
                        "frame " + i + ": width mismatch");
                assertEquals(
                        HEIGHT,
                        captured.colorImage.getMat().rows(),
                        "frame " + i + ": height mismatch");
                assertEquals(
                        3,
                        captured.colorImage.getMat().channels(),
                        "frame " + i + ": expected 3-channel BGR");
                assertEquals(
                        CAPTURE_NS[i],
                        captured.captureTimestamp,
                        "frame " + i + ": captureTimestamp must be source-side capture_ns verbatim");

                captured.colorImage.release();
            }

            // After EOF: next read should return an empty frame and flip isConnected to false.
            var post = provider.getInputMat();
            assertTrue(
                    post.colorImage.getMat().empty(),
                    "post-EOF read should produce an empty Mat");
            assertFalse(provider.isConnected(), "should report disconnected after EOF");
            post.colorImage.release();

            // Repeat EOF call: must still short-circuit, not throw.
            var post2 = provider.getInputMat();
            assertTrue(post2.colorImage.getMat().empty());
            post2.colorImage.release();
        } finally {
            provider.release();
        }
    }

    @Test
    void refusesPre2183RecordingMissingSidecar(@TempDir Path tmp) throws IOException {
        // Copy the synthesized frames/ subtree but not metadata.jsonl. The provider should
        // refuse because without source-side capture_ns we can't honour the replay timing
        // contract.
        Path dir = tmp.resolve("pre-2183");
        Path framesCopy = dir.resolve("frames");
        Files.createDirectories(framesCopy);
        try (var stream = Files.list(recordingDir.resolve("frames"))) {
            stream.forEach(
                    src -> {
                        try {
                            Files.copy(src, framesCopy.resolve(src.getFileName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        // intentionally no metadata.jsonl

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(
                ex.getMessage().toLowerCase().contains("metadata.jsonl"),
                "error should name the missing file; got: " + ex.getMessage());
    }

    @Test
    void refusesMissingFrames(@TempDir Path tmp) throws IOException {
        // Sidecar present, frames/ absent. Could be an old MJPEG-AVI / mp4 recording, could be
        // a writer that crashed before the first frame. Either way the provider refuses.
        Path dir = tmp.resolve("no-frames");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("metadata.jsonl"), "{\"seq\":0,\"capture_ns\":1}\n");

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(
                ex.getMessage().toLowerCase().contains("frames"),
                "error should name the missing dir; got: " + ex.getMessage());
    }

    @Test
    void refusesEmptyFramesDir(@TempDir Path tmp) throws IOException {
        // frames/ exists but contains no 000000.jpg — recorder crashed between mkdir and the
        // first frame write. Refuse so the user doesn't get silent garbage replay.
        Path dir = tmp.resolve("empty-frames");
        Files.createDirectories(dir.resolve("frames"));
        Files.writeString(dir.resolve("metadata.jsonl"), "{\"seq\":0,\"capture_ns\":1}\n");

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(
                ex.getMessage().toLowerCase().contains("first frame")
                        || ex.getMessage().toLowerCase().contains("missing"),
                "error should indicate the empty frames dir; got: " + ex.getMessage());
    }

    @Test
    void getInputMatMarksConnectedEvenIfIsConnectedNeverCalled() throws IOException {
        // Defensive: VisionRunner always calls isConnected() before getInputMat, but tests and
        // future callers may not. The first getInputMat must still mark the source as having
        // connected so hasConnected() reports the truth.
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertFalse(provider.hasConnected(), "no calls yet — flag should still be false");
            var captured = provider.getInputMat();
            assertFalse(captured.colorImage.getMat().empty());
            assertTrue(
                    provider.hasConnected(),
                    "first getInputMat must flip the cameraPropertiesCached flag");
            captured.colorImage.release();
        } finally {
            provider.release();
        }
    }

    @Test
    void setRecordingThrows() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertThrows(UnsupportedOperationException.class, () -> provider.setRecording(true));
            assertFalse(provider.getRecording());
        } finally {
            provider.release();
        }
    }
}

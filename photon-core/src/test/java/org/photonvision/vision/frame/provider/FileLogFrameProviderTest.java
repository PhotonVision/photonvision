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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.jni.LibraryLoader;

/**
 * Integration-ish test for {@link FileLogFrameProvider}. Synthesises a tiny H.264 mp4 with
 * ffmpeg, pairs it with a hand-written sidecar, then walks it through the provider and asserts
 * frame count, dimensions, channel order, and timestamp propagation.
 *
 * <p>Requires the OpenCV native libraries (loaded in {@code @BeforeAll} via
 * {@link LibraryLoader#loadWpiLibraries()}) and the {@code ffmpeg} binary on {@code PATH}.
 * The latter is gated by {@link Assumptions#assumeTrue} so the test skips gracefully on
 * environments without ffmpeg rather than failing.
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
    static boolean ffmpegAvailable;

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        if (!LibraryLoader.loadWpiLibraries()) {
            fail("Failed to load WPI / OpenCV native libraries");
        }

        ffmpegAvailable = isFfmpegOnPath();
        if (!ffmpegAvailable) {
            return; // each test will skip via assumeTrue
        }

        recordingDir = sharedTempDir.resolve("rec");
        Files.createDirectories(recordingDir);

        synthesizeRecordingMp4(recordingDir.resolve("recording.mp4"));
        writeSidecar(recordingDir.resolve("metadata.jsonl"));
    }

    private static boolean isFfmpegOnPath() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /** Generate a deterministic 3-frame H.264 mp4 the same way FrameRecorder does in production. */
    private static void synthesizeRecordingMp4(Path out) throws IOException, InterruptedException {
        // lavfi testsrc2: deterministic synthetic pattern, fast to encode. 3 frames at 30 fps.
        // libx264 ultrafast+zerolatency mirrors FrameRecorder.java's encoder args so the decode
        // path we test here matches what production writes.
        ProcessBuilder pb =
                new ProcessBuilder(
                        "ffmpeg",
                        "-y",
                        "-f",
                        "lavfi",
                        "-i",
                        "testsrc2=size=" + WIDTH + "x" + HEIGHT + ":rate=30",
                        "-frames:v",
                        Integer.toString(CAPTURE_NS.length),
                        "-c:v",
                        "libx264",
                        "-preset",
                        "ultrafast",
                        "-tune",
                        "zerolatency",
                        "-pix_fmt",
                        "yuv420p",
                        out.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        byte[] log = p.getInputStream().readAllBytes();
        int rc = p.waitFor();
        if (rc != 0) {
            fail(
                    "ffmpeg failed (exit "
                            + rc
                            + "). Output:\n"
                            + new String(log, StandardCharsets.UTF_8));
        }
        assertTrue(Files.size(out) > 0, "ffmpeg produced empty mp4");
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
        Assumptions.assumeTrue(ffmpegAvailable, "ffmpeg not on PATH; skipping decode round-trip test");

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
        Assumptions.assumeTrue(
                ffmpegAvailable, "ffmpeg not on PATH; can't synthesise mp4 fixture");

        Path dir = tmp.resolve("pre-2183");
        Files.createDirectories(dir);
        Files.copy(recordingDir.resolve("recording.mp4"), dir.resolve("recording.mp4"));
        // intentionally no metadata.jsonl

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(
                ex.getMessage().toLowerCase().contains("metadata.jsonl"),
                "error should name the missing file; got: " + ex.getMessage());
    }

    @Test
    void refusesMissingMp4(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("no-mp4");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("metadata.jsonl"), "{\"seq\":0,\"capture_ns\":1}\n");

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(
                ex.getMessage().toLowerCase().contains("recording.mp4"),
                "error should name the missing file; got: " + ex.getMessage());
    }

    @Test
    void getInputMatMarksConnectedEvenIfIsConnectedNeverCalled() throws IOException {
        // Defensive: VisionRunner always calls isConnected() before getInputMat, but tests and
        // future callers may not. The first getInputMat must still mark the source as having
        // connected so hasConnected() reports the truth.
        Assumptions.assumeTrue(ffmpegAvailable, "ffmpeg not on PATH");
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
        Assumptions.assumeTrue(ffmpegAvailable, "ffmpeg not on PATH");
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertThrows(UnsupportedOperationException.class, () -> provider.setRecording(true));
            assertFalse(provider.getRecording());
        } finally {
            provider.release();
        }
    }
}

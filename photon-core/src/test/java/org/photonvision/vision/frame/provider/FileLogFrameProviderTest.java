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
import org.photonvision.vision.frame.provider.CpuImageProcessor.CapturedFrame;

/**
 * Synthesises a 3-frame image-sequence fixture via {@link Imgcodecs#imwrite} (the same encoder path
 * {@code FrameRecorder} uses) and walks it through {@link FileLogFrameProvider}. Needs OpenCV but
 * not {@code photontargetingJNI}, so runs on any platform.
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

    private static void synthesizeFrames(Path framesDir) throws IOException {
        Files.createDirectories(framesDir);
        for (int i = 0; i < CAPTURE_NS.length; i++) {
            Mat frame = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3, new Scalar(i * 64 % 255, 64, 128));
            try {
                Path out = FrameLogFormat.framePath(framesDir, i);
                assertTrue(Imgcodecs.imwrite(out.toString(), frame), "Imgcodecs.imwrite failed for " + out);
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
                assertEquals(WIDTH, captured.colorImage.getMat().cols(), "frame " + i + ": width mismatch");
                assertEquals(
                        HEIGHT, captured.colorImage.getMat().rows(), "frame " + i + ": height mismatch");
                assertEquals(
                        3, captured.colorImage.getMat().channels(), "frame " + i + ": expected 3-channel BGR");
                assertEquals(
                        CAPTURE_NS[i],
                        captured.captureTimestamp,
                        "frame " + i + ": captureTimestamp must be source-side capture_ns verbatim");

                captured.colorImage.release();
            }

            // After EOF the provider parks the vision thread instead of disconnecting — the
            // MJPEG stream holds whatever the browser last received, the pipeline stops running,
            // and the "Frames Processed" counter freezes. To restart the user deactivates and
            // reactivates the camera. See parksTheVisionThreadAtEof below for the wake-on-interrupt
            // contract; here we just verify isConnected stays true past EOF.
            assertTrue(provider.isConnected(), "replay source should stay connected after EOF");
        } finally {
            provider.release();
        }
    }

    @Test
    void parksTheVisionThreadAtEof() throws Exception {
        // After consuming the recording, getInputMat must block until interrupted (mirroring
        // VisionRunner.stopProcess's path) so the pipeline stops running on deactivation rather
        // than spinning on a frozen frame. Run getInputMat on a separate thread, confirm it
        // blocks, then interrupt it and verify it wakes with an empty CVMat + preserved flag.
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            // Burn through all frames so the next call hits EOF.
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                provider.getInputMat().colorImage.release();
            }

            java.util.concurrent.atomic.AtomicReference<CapturedFrame> result =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean interruptedAfter =
                    new java.util.concurrent.atomic.AtomicBoolean();
            Thread t =
                    new Thread(
                            () -> {
                                result.set(provider.getInputMat());
                                interruptedAfter.set(Thread.currentThread().isInterrupted());
                            });
            t.setName("parksTheVisionThreadAtEof-runner");
            t.start();

            Thread.sleep(250); // give it time to enter the park
            assertTrue(t.isAlive(), "getInputMat should park after EOF, not return");

            t.interrupt();
            t.join(2_000);
            assertFalse(t.isAlive(), "getInputMat should wake within 2s of interrupt");

            CapturedFrame captured = result.get();
            assertNotNull(captured, "returned CapturedFrame should not be null");
            assertTrue(
                    captured.colorImage.getMat().empty(),
                    "post-interrupt CapturedFrame should carry an empty Mat");
            assertTrue(
                    interruptedAfter.get(),
                    "interrupt flag must be preserved so VisionRunner's outer loop exits cleanly");
            captured.colorImage.release();
        } finally {
            provider.release();
        }
    }

    @Test
    void refusesRecordingMissingSidecar(@TempDir Path tmp) throws IOException {
        // Frames/ copied, no metadata.jsonl.
        Path dir = tmp.resolve("no-sidecar");
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
        // Sidecar present, frames/ absent.
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
        // frames/ exists, 000000.jpg absent.
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
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertFalse(provider.hasConnected(), "no calls yet — flag should still be false");
            var captured = provider.getInputMat();
            assertFalse(captured.colorImage.getMat().empty());
            assertTrue(
                    provider.hasConnected(), "first getInputMat must flip the cameraPropertiesCached flag");
            captured.colorImage.release();
        } finally {
            provider.release();
        }
    }

    @Test
    void getTotalFramesMatchesFixture() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            assertEquals(
                    CAPTURE_NS.length,
                    provider.getTotalFrames(),
                    "totalFrames must reflect the on-disk frames/*.jpg count");
        } finally {
            provider.release();
        }
    }

    @Test
    void onProgressFiresOncePerEmittedFrame() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            java.util.List<Long> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
            provider.setOnProgress(seen::add);

            for (int i = 0; i < CAPTURE_NS.length; i++) {
                provider.getInputMat().colorImage.release();
            }

            assertEquals(CAPTURE_NS.length, seen.size(), "one callback per emitted frame");
            for (int i = 0; i < seen.size(); i++) {
                assertEquals(
                        (long) (i + 1),
                        (long) seen.get(i),
                        "callback arg should be the running 1-based emitted-frame count");
            }
        } finally {
            provider.release();
        }
    }

    @Test
    void onEofFiresOnceOnNaturalExhaustion() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            java.util.concurrent.atomic.AtomicInteger fired =
                    new java.util.concurrent.atomic.AtomicInteger();
            provider.setOnEof(fired::incrementAndGet);

            // Burn through all frames.
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                provider.getInputMat().colorImage.release();
            }
            assertEquals(0, fired.get(), "onEof must not fire while frames remain");

            // Next read exhausts metadata.jsonl → enters stopped state, fires callback exactly once.
            provider.getInputMat().colorImage.release();
            assertEquals(1, fired.get(), "onEof must fire on natural exhaustion");

            // Subsequent reads must not fire it again.
            provider.getInputMat().colorImage.release();
            provider.getInputMat().colorImage.release();
            assertEquals(1, fired.get(), "onEof must be one-shot");
        } finally {
            provider.release();
        }
    }

    @Test
    void requestStopShortCircuitsAndFiresEofOnce() throws IOException {
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            java.util.concurrent.atomic.AtomicInteger fired =
                    new java.util.concurrent.atomic.AtomicInteger();
            provider.setOnEof(fired::incrementAndGet);

            provider.requestStop();
            assertEquals(1, fired.get(), "requestStop must fire onEof synchronously");

            // Next getInputMat should return an empty frame (callback wired ⇒ no parking).
            var captured = provider.getInputMat();
            assertTrue(
                    captured.colorImage.getMat().empty(),
                    "after requestStop, getInputMat must return empty for swap-aware consumers");
            captured.colorImage.release();

            // Second requestStop is a no-op via the CAS.
            provider.requestStop();
            assertEquals(1, fired.get(), "requestStop must be idempotent");
        } finally {
            provider.release();
        }
    }

    @Test
    void eofWithoutCallbackPreservesParkingBehaviour() throws Exception {
        // Standalone use (no setOnEof): the original "park on EOF, deactivate to restart" semantics
        // must be preserved so non-swap-aware consumers don't busy-loop on empties.
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                provider.getInputMat().colorImage.release();
            }

            Thread t =
                    new Thread(
                            () -> {
                                provider.getInputMat().colorImage.release();
                            });
            t.setName("eofWithoutCallback-runner");
            t.start();
            Thread.sleep(150);
            assertTrue(t.isAlive(), "no callback wired ⇒ should park at EOF, not return");
            t.interrupt();
            t.join(2_000);
            assertFalse(t.isAlive(), "interrupt must wake the parked thread");
        } finally {
            provider.release();
        }
    }

    @Test
    void setRecordingIsNoOp() throws IOException {
        // setRecording is invoked by the NT listener thread (NTDataPublisher routes
        // recordingRequest writes through FrameProvider::setRecording with no try/catch).
        // Replay sources must not throw — they silently ignore the request and stay false.
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            provider.setRecording(true);
            assertFalse(provider.getRecording());
            provider.setRecording(false);
            assertFalse(provider.getRecording());
        } finally {
            provider.release();
        }
    }
}

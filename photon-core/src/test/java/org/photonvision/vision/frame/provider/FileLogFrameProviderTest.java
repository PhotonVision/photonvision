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
import org.photonvision.vision.pipeline.FrameRecorder;

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
                Path out = FrameRecorder.framePath(framesDir, i);
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

            // The provider never "disconnects": isConnected stays true past EOF so the swap-back
            // (driven by the onEof callback, never a connection loss) is the only exit path.
            assertTrue(provider.isConnected(), "replay source should stay connected after EOF");
        } finally {
            provider.release();
        }
    }

    @Test
    void throwsAtEofWhenNoCallbackWired() throws IOException {
        // Reaching the stopped state without setOnEof is a wiring bug — VisionModule.startReplay
        // always wires the callback before installing the provider. Fail loudly rather than
        // starve the vision thread on empty frames forever.
        FileLogFrameProvider provider = new FileLogFrameProvider(recordingDir);
        try {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                provider.getInputMat().colorImage.release();
            }
            assertThrows(IllegalStateException.class, provider::getInputMat);
        } finally {
            provider.release();
        }
    }

    @Test
    void refusesRecordingMissingSidecar(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("no-sidecar");
        Files.createDirectories(dir.resolve("frames"));
        Files.copy(
                recordingDir.resolve("frames").resolve("000000.jpg"),
                dir.resolve("frames").resolve("000000.jpg"));

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(ex.getMessage().toLowerCase().contains("metadata.jsonl"), ex.getMessage());
    }

    @Test
    void refusesMissingFrames(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("no-frames");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("metadata.jsonl"), "{\"seq\":0,\"capture_ns\":1}\n");

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(ex.getMessage().toLowerCase().contains("frames"), ex.getMessage());
    }

    @Test
    void refusesEmptyFramesDir(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("empty-frames");
        Files.createDirectories(dir.resolve("frames"));
        Files.writeString(dir.resolve("metadata.jsonl"), "{\"seq\":0,\"capture_ns\":1}\n");

        IOException ex = assertThrows(IOException.class, () -> new FileLogFrameProvider(dir));
        assertTrue(ex.getMessage().toLowerCase().contains("missing"), ex.getMessage());
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
    void requestStopUnparksAPacedRead(@TempDir Path tmp) throws Exception {
        // Two frames 30 s apart: the second getInputMat parks to honour the recorded delta.
        // requestStop must wake it promptly instead of waiting out the full gap.
        Path dir = tmp.resolve("long-gap");
        Path frames = dir.resolve("frames");
        Files.createDirectories(frames);
        Files.copy(
                FrameRecorder.framePath(recordingDir.resolve("frames"), 0),
                FrameRecorder.framePath(frames, 0));
        Files.copy(
                FrameRecorder.framePath(recordingDir.resolve("frames"), 1),
                FrameRecorder.framePath(frames, 1));
        try (BufferedWriter w =
                Files.newBufferedWriter(dir.resolve("metadata.jsonl"), StandardCharsets.UTF_8)) {
            w.write("{\"seq\":0,\"capture_ns\":0}\n");
            w.write("{\"seq\":1,\"capture_ns\":30000000000}\n");
        }

        FileLogFrameProvider provider = new FileLogFrameProvider(dir);
        try {
            provider.setOnEof(() -> {});
            provider.getInputMat().colorImage.release(); // anchors pacing

            Thread t =
                    new Thread(
                            () -> {
                                provider.getInputMat().colorImage.release();
                            });
            t.setName("requestStopUnparksAPacedRead-runner");
            t.start();
            Thread.sleep(250); // give it time to enter the paced park
            assertTrue(t.isAlive(), "second read should be parked honouring the recorded 30s gap");

            provider.requestStop();
            t.join(2_000);
            assertFalse(t.isAlive(), "requestStop must unpark the paced read promptly");
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

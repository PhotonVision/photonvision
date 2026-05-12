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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.opencv.CVMat;

/**
 * Scratch smoke test: feeds 60 synthetic 640x480 BGR frames through FrameRecorder and verifies the
 * recording.avi + metadata sidecar by reading them back through OpenCV's VideoCapture (the same
 * code path FileLogFrameProvider uses on replay). Uses the package-private DI ctor so it doesn't
 * need photontargetingJNI. Delete after manual verification.
 *
 * <p>Platform-independent — relies only on OpenCV's CV_MJPEG built-in backend, no ffmpeg/ffprobe.
 */
public class FrameRecorderSmokeTest {
    private static final int W = 640;
    private static final int H = 480;
    private static final int FRAME_COUNT = 60;

    @BeforeAll
    static void init() {
        // OpenCV + WPI bits we need (HAL, NT JNI not actually used because the DI ctor bypasses
        // singletons). loadTargeting() is intentionally skipped.
        if (!LibraryLoader.loadWpiLibraries()) fail("loadWpiLibraries() returned false");
    }

    @Test
    public void recordsSyntheticFramesIntoMjpegAvi(@TempDir Path tempDir) throws Exception {
        Path outDir = tempDir.resolve("rec1");
        Files.createDirectories(outDir);

        FrameRecorder recorder =
                new FrameRecorder(outDir, FrameRecorder.RecordingStrategy.VIDEO, Long.MAX_VALUE);
        try {
            assertTrue(recorder.startRecording(), "startRecording() should succeed first call");

            long baseNs = 1_000_000_000L; // pretend wall-clock origin
            for (int i = 0; i < FRAME_COUNT; i++) {
                // Solid-color BGR frame that shifts each frame so the encoder sees real motion.
                Mat mat = new Mat(H, W, CvType.CV_8UC3, new Scalar(i * 4 % 255, 64, 128));
                CVMat cv = new CVMat(mat);

                long captureNs = baseNs + i * 33_333_333L; // 30fps cadence
                boolean queued = recorder.recordFrame(cv, captureNs);
                assertTrue(queued, "frame " + i + " not queued");

                // Confirm clone semantics: caller's Mat is still alive after recordFrame.
                assertTrue(!mat.empty(), "caller's mat was released by recordFrame (use-after-free)");

                cv.release(); // releases caller's Mat (mirrors real pipeline behavior)

                // Pace slower than the 30-deep queue can encode (~5ms each = ~300ms total).
                Thread.sleep(5);
            }
        } finally {
            recorder.release();
        }

        // --- Assertions on disk artifacts ---
        Path avi = outDir.resolve("recording.avi");
        Path meta = outDir.resolve("metadata.jsonl");
        Path strat = outDir.resolve("strat");

        assertTrue(Files.exists(avi), "recording.avi missing");
        assertTrue(Files.size(avi) > 0, "recording.avi is empty");
        assertTrue(Files.exists(meta), "metadata.jsonl missing");
        assertTrue(Files.exists(strat), "strat file missing");
        assertEquals("VIDEO", Files.readString(strat, StandardCharsets.UTF_8).trim());

        // Decode the AVI with OpenCV — verifies the SAME code path replay will exercise.
        VideoCapture cap = new VideoCapture(avi.toString());
        try {
            assertTrue(cap.isOpened(), "VideoCapture could not open the produced recording.avi");
            int width = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
            int height = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            assertEquals(W, width, "decoded width mismatch");
            assertEquals(H, height, "decoded height mismatch");

            int decodedCount = 0;
            Mat frame = new Mat();
            while (cap.read(frame)) {
                if (frame.empty()) break;
                decodedCount++;
                assertEquals(3, frame.channels(), "frame " + decodedCount + ": expected BGR (3 channels)");
            }
            frame.release();
            assertEquals(FRAME_COUNT, decodedCount, "decoded frame count mismatch");
        } finally {
            cap.release();
        }

        // Sidecar must have at least as many lines as decoded frames (the writer's invariant —
        // jsonl flushed before frame).
        List<String> lines;
        try (BufferedReader r = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
            lines = r.lines().toList();
        }
        assertTrue(
                lines.size() >= FRAME_COUNT,
                "metadata.jsonl line count " + lines.size() + " < frame count " + FRAME_COUNT);
        // Trim to the frame count we decoded (the writer's contract for readers).
        assertFalse(lines.isEmpty());
        for (int i = 0; i < FRAME_COUNT; i++) {
            String expected =
                    "{\"seq\":" + i + ",\"capture_ns\":" + (1_000_000_000L + i * 33_333_333L) + "}";
            assertEquals(expected, lines.get(i), "metadata line " + i + " mismatch");
        }

        System.out.println(
                "SMOKE OK: "
                        + FRAME_COUNT
                        + " frames -> "
                        + Files.size(avi)
                        + " bytes MJPEG-AVI at "
                        + W
                        + "x"
                        + H
                        + ", metadata seq+capture_ns verbatim, round-trip decode via VideoCapture.");
    }
}

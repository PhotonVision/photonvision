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
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.opencv.CVMat;

/**
 * Round-trips 60 synthetic frames through {@link FrameRecorder} and decodes each back with {@link
 * Imgcodecs} — the same decoder path {@code FileLogFrameProvider} uses on replay. Uses the
 * package-private DI ctor so it doesn't need {@code photontargetingJNI}.
 */
public class FrameRecorderIntegrationTest {
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
    public void recordsSyntheticFramesIntoJpegSequence(@TempDir Path tempDir) throws Exception {
        Path outDir = tempDir.resolve("rec1");
        Files.createDirectories(outDir);

        FrameRecorder recorder =
                new FrameRecorder(outDir, Long.MAX_VALUE, FrameRecorder.TssSample.INACTIVE);
        try {
            recorder.startRecording();

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
        Path framesDir = outDir.resolve("frames");
        Path meta = outDir.resolve("metadata.jsonl");

        assertTrue(Files.isDirectory(framesDir), "frames/ directory missing");
        assertTrue(Files.exists(meta), "metadata.jsonl missing");

        for (int i = 0; i < FRAME_COUNT; i++) {
            Path framePath = FrameRecorder.framePath(framesDir, i);
            assertTrue(Files.exists(framePath), "frame " + i + " missing: " + framePath);
            assertTrue(Files.size(framePath) > 0, "frame " + i + " is empty");

            Mat decoded = Imgcodecs.imread(framePath.toString());
            try {
                assertFalse(decoded.empty(), "frame " + i + " decoded empty");
                assertEquals(W, decoded.cols(), "frame " + i + ": width mismatch");
                assertEquals(H, decoded.rows(), "frame " + i + ": height mismatch");
                assertEquals(3, decoded.channels(), "frame " + i + ": expected BGR (3 channels)");
            } finally {
                decoded.release();
            }
        }

        List<String> lines;
        try (BufferedReader r = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
            lines = r.lines().toList();
        }
        assertTrue(
                lines.size() >= FRAME_COUNT,
                "metadata.jsonl line count " + lines.size() + " < frame count " + FRAME_COUNT);
        for (int i = 0; i < FRAME_COUNT; i++) {
            String expected =
                    "{\"seq\":" + i + ",\"capture_ns\":" + (1_000_000_000L + i * 33_333_333L) + "}";
            assertEquals(expected, lines.get(i), "metadata line " + i + " mismatch");
        }
    }
}

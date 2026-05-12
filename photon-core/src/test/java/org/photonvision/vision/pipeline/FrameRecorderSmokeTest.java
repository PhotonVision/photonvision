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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.opencv.CVMat;

/**
 * Scratch smoke test: feeds 60 synthetic 640x480 BGR frames through FrameRecorder and verifies the
 * .mp4 + metadata sidecar. NOT for CI — requires ffmpeg+ffprobe on PATH. Uses the package-private
 * DI ctor so it doesn't need photontargetingJNI (not buildable here without MSVC). Delete after
 * manual verification.
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
    public void recordsSyntheticFramesThroughFfmpeg(@TempDir Path tempDir) throws Exception {
        assertTrue(commandAvailable("ffmpeg"), "ffmpeg not on PATH");
        assertTrue(commandAvailable("ffprobe"), "ffprobe not on PATH");

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
        Path mp4 = outDir.resolve("recording.mp4");
        Path meta = outDir.resolve("metadata.jsonl");
        Path strat = outDir.resolve("strat");

        assertTrue(Files.exists(mp4), "recording.mp4 missing");
        assertTrue(Files.size(mp4) > 0, "recording.mp4 is empty");
        assertTrue(Files.exists(meta), "metadata.jsonl missing");
        assertTrue(Files.exists(strat), "strat file missing");
        assertEquals("VIDEO", Files.readString(strat, StandardCharsets.UTF_8).trim());

        FfprobeResult probe = ffprobe(mp4);
        assertEquals("h264", probe.codec, "expected H.264 in the .mp4");
        assertEquals(W, probe.width);
        assertEquals(H, probe.height);
        assertEquals(FRAME_COUNT, probe.nbFrames, "encoded frame count mismatch");

        List<String> lines;
        try (BufferedReader r = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
            lines = r.lines().toList();
        }
        assertEquals(FRAME_COUNT, lines.size(), "metadata.jsonl line count mismatch");
        for (int i = 0; i < FRAME_COUNT; i++) {
            String expected =
                    "{\"seq\":" + i + ",\"capture_ns\":" + (1_000_000_000L + i * 33_333_333L) + "}";
            assertEquals(expected, lines.get(i), "metadata line " + i + " mismatch");
        }

        System.out.println(
                "SMOKE OK: "
                        + FRAME_COUNT
                        + " frames -> "
                        + Files.size(mp4)
                        + " bytes h264 at "
                        + W
                        + "x"
                        + H
                        + ", metadata seq+capture_ns verbatim.");
    }

    // --- helpers ---

    private static boolean commandAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "-version").redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private record FfprobeResult(String codec, int width, int height, int nbFrames) {}

    private static FfprobeResult ffprobe(Path mp4) throws Exception {
        // -count_frames forces actual frame decode rather than relying on container metadata.
        Process p =
                new ProcessBuilder(
                                "ffprobe",
                                "-v",
                                "error",
                                "-select_streams",
                                "v:0",
                                "-count_frames",
                                "-show_entries",
                                "stream=codec_name,width,height,nb_read_frames",
                                "-of",
                                "default=noprint_wrappers=1",
                                mp4.toString())
                        .redirectErrorStream(true)
                        .start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(15, TimeUnit.SECONDS), "ffprobe hung");
        assertEquals(0, p.exitValue(), "ffprobe failed:\n" + out);

        String codec = null;
        int width = -1, height = -1, nb = -1;
        for (String line : out.split("\\R")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "codec_name" -> codec = kv[1];
                case "width" -> width = Integer.parseInt(kv[1]);
                case "height" -> height = Integer.parseInt(kv[1]);
                case "nb_read_frames" -> nb = Integer.parseInt(kv[1]);
            }
        }
        assertNotNull(codec, "ffprobe didn't report codec_name; out:\n" + out);
        return new FfprobeResult(codec, width, height, nb);
    }
}

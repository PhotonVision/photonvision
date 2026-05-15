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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PnpResult;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;

/**
 * End-to-end determinism verification. Records a synthetic recording with a known TSS snapshot,
 * then runs two exporter passes (different "settings") against pipeline results whose timestamps
 * are pulled straight from the recording's metadata.jsonl. Verifies the two contracts the AKit
 * consumer relies on:
 *
 * <ol>
 *   <li>No tuning change ⇒ same {@code capture_ns} sequence in JSON as in the recording's
 *       metadata.jsonl, byte-for-byte deterministic given a fixed publish clock.
 *   <li>Tuning change ⇒ same {@code capture_ns} sequence but different result payloads + a
 *       different output filename (per-pipeline-hash isolation).
 * </ol>
 *
 * <p>FileLogFrameProvider's metadata round-trip is verified separately by {@code
 * FileLogFrameProviderTest.emitsFramesWithCaptureTimestamps}; here we read {@code metadata.jsonl}
 * directly to keep the test in the {@code vision.pipeline} package.
 */
public class JsonResultEndToEndTest {
    private static final int W = 64;
    private static final int H = 48;
    private static final long[] CAPTURE_NS = {
        2_000_000_000L, 2_033_333_333L, 2_066_666_666L, 2_100_000_000L
    };
    private static final long TSS_OFFSET_NS = 12_345_000L; // ~12 ms offset
    private static final long FIXED_PUBLISH_MICROS = 555_000_000L;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void init() {
        if (!LibraryLoader.loadWpiLibraries()) fail("loadWpiLibraries() returned false");
    }

    @Test
    public void recordReplayExportRoundTripIsDeterministicAndTuningSensitive(@TempDir Path tempDir)
            throws Exception {
        // --- 1. Record a synthetic recording with a known TSS snapshot. ---
        Path recordingDir = tempDir.resolve("rec");
        Files.createDirectories(recordingDir);
        var tss = new FrameRecorder.TssSample(true, TSS_OFFSET_NS);

        FrameRecorder recorder =
                new FrameRecorder(
                        recordingDir, FrameRecorder.RecordingStrategy.SNAPSHOTS, Long.MAX_VALUE, tss);
        try {
            assertTrue(recorder.startRecording());
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                Mat mat = new Mat(H, W, CvType.CV_8UC3, new Scalar(i * 30 % 255, 64, 128));
                CVMat cv = new CVMat(mat);
                assertTrue(recorder.recordFrame(cv, CAPTURE_NS[i]));
                cv.release();
                Thread.sleep(5);
            }
        } finally {
            recorder.release();
        }

        // Confirm the recording landed on disk as expected — metadata.jsonl drives our assertions.
        Path metadataPath = recordingDir.resolve("metadata.jsonl");
        assertTrue(Files.exists(metadataPath));
        assertTrue(Files.exists(recordingDir.resolve("tss.json")));
        List<Long> recordedCaptureNs = readCaptureNsFromMetadata(metadataPath);
        assertEquals(CAPTURE_NS.length, recordedCaptureNs.size());

        // --- 2. Pull per-frame timestamps from metadata.jsonl. (Replay-side round-trip is
        // already covered by FileLogFrameProviderTest.emitsFramesWithCaptureTimestamps.) ---
        List<Long> replayedCaptureNs = recordedCaptureNs;

        // --- 3. Export pass A: settings X, no targets in results. ---
        var snapshot = JsonResultExporter.readSnapshot(recordingDir);
        assertTrue(snapshot.isPresent());
        assertEquals(TSS_OFFSET_NS, snapshot.get().tssOffsetAtRecordNs(), "snapshot must round-trip");

        // Frame window read from the recording's metadata.jsonl. All CAPTURE_NS values are
        // inside this window by construction, so no result should be filtered by the bounds
        // guard — but exercising the real readFrameWindow path keeps this test honest end-to-end.
        var frameWindow = JsonResultExporter.readFrameWindow(recordingDir);
        assertTrue(frameWindow.isPresent());
        assertEquals(CAPTURE_NS[0], frameWindow.get().firstCaptureNs());
        assertEquals(CAPTURE_NS[CAPTURE_NS.length - 1], frameWindow.get().lastCaptureNs());

        var settingsX = new AprilTagPipelineSettings();
        Path fileX = recordingDir.resolve("results").resolve("X.jsonl");
        try (var exporter =
                new JsonResultExporter(
                        fileX, "cam", "rec", settingsX, snapshot, frameWindow, () -> FIXED_PUBLISH_MICROS)) {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                exporter.accept(buildResult(i, replayedCaptureNs.get(i), Optional.empty()));
            }
        }

        // --- 4. Export pass B: settings Y (different hash), WITH multitag payload. ---
        var settingsY = new AprilTagPipelineSettings();
        settingsY.decimate = settingsX.decimate + 7;
        assertNotEquals(settingsX.hashCode(), settingsY.hashCode());
        var multitag =
                Optional.of(
                        new MultiTargetPNPResult(
                                new PnpResult(
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(0.1, 0.2, 0.3)),
                                        0.05),
                                List.of((short) 1, (short) 2)));
        Path fileY = recordingDir.resolve("results").resolve("Y.jsonl");
        try (var exporter =
                new JsonResultExporter(
                        fileY, "cam", "rec", settingsY, snapshot, frameWindow, () -> FIXED_PUBLISH_MICROS)) {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                exporter.accept(buildResult(i, replayedCaptureNs.get(i), multitag));
            }
        }

        // --- 5. Assertions. ---
        List<JsonNode> linesX = readLines(fileX);
        List<JsonNode> linesY = readLines(fileY);
        assertEquals(CAPTURE_NS.length + 1, linesX.size(), "header + N results");
        assertEquals(linesX.size(), linesY.size());

        // Headers: same camera/recording, DIFFERENT pipeline_hash, identical tss fields.
        JsonNode headerX = linesX.get(0);
        JsonNode headerY = linesY.get(0);
        assertNotEquals(
                headerX.get("pipeline_hash").asText(),
                headerY.get("pipeline_hash").asText(),
                "different settings must produce different pipeline_hash");
        assertEquals(
                headerX.get("tss_offset_at_record_ns").asLong(),
                headerY.get("tss_offset_at_record_ns").asLong());

        // Per-frame: same capture_ns/seq across passes, but different decoded packet contents.
        for (int i = 1; i <= CAPTURE_NS.length; i++) {
            JsonNode lineX = linesX.get(i);
            JsonNode lineY = linesY.get(i);
            assertEquals(
                    lineX.get("capture_ns").asLong(),
                    lineY.get("capture_ns").asLong(),
                    "tuning change must not perturb capture_ns at line " + i);
            assertEquals(lineX.get("seq").asLong(), lineY.get("seq").asLong());
            assertEquals(
                    CAPTURE_NS[i - 1],
                    lineX.get("capture_ns").asLong(),
                    "line " + i + " capture_ns must match recording's metadata.jsonl");

            PhotonPipelineResult resultX = decodePacket(lineX);
            PhotonPipelineResult resultY = decodePacket(lineY);
            assertFalse(resultX.getMultiTagResult().isPresent(), "pass X carried no multitag payload");
            assertTrue(resultY.getMultiTagResult().isPresent(), "pass Y carried a multitag payload");

            // Embedded packet capture timestamp is TSS-shifted: captureNs/1000 + offsetUs.
            long expectedCaptureMicros = CAPTURE_NS[i - 1] / 1_000L + TSS_OFFSET_NS / 1_000L;
            assertEquals(expectedCaptureMicros, resultX.metadata.captureTimestampMicros);
            assertEquals(expectedCaptureMicros, resultY.metadata.captureTimestampMicros);
        }

        // Re-export pass A under the same fixed clock + inputs ⇒ byte-identical file. The
        // determinism property AKit consumers rely on for re-runnable replays.
        Path fileXReplay = tempDir.resolve("X-replay.jsonl");
        try (var exporter =
                new JsonResultExporter(
                        fileXReplay,
                        "cam",
                        "rec",
                        settingsX,
                        snapshot,
                        frameWindow,
                        () -> FIXED_PUBLISH_MICROS)) {
            for (int i = 0; i < CAPTURE_NS.length; i++) {
                exporter.accept(buildResult(i, replayedCaptureNs.get(i), Optional.empty()));
            }
        }
        assertEquals(
                Files.readString(fileX),
                Files.readString(fileXReplay),
                "two exporter runs with identical inputs + fixed clock must be byte-identical");
    }

    // -------- helpers --------

    private static CVPipelineResult buildResult(
            int seq, long captureNs, Optional<MultiTargetPNPResult> multitag) {
        var r = new CVPipelineResult(seq, 1_000_000.0, 30.0, List.of(), multitag);
        r.setImageCaptureTimestampNanos(captureNs);
        return r;
    }

    private static List<JsonNode> readLines(Path file) throws Exception {
        List<JsonNode> out = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            out.add(JSON.readTree(line));
        }
        return out;
    }

    private static List<Long> readCaptureNsFromMetadata(Path metadata) throws Exception {
        List<Long> out = new ArrayList<>();
        for (String line : Files.readAllLines(metadata)) {
            out.add(JSON.readTree(line).get("capture_ns").asLong());
        }
        return out;
    }

    private static PhotonPipelineResult decodePacket(JsonNode line) {
        byte[] bytes = Base64.getDecoder().decode(line.get("packet_b64").asText());
        return PhotonPipelineResult.photonStruct.unpack(new Packet(bytes));
    }
}

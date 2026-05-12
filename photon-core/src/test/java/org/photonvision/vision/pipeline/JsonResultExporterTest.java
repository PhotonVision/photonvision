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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PnpResult;
import org.photonvision.vision.pipeline.JsonResultExporter.OffsetSnapshot;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;

/**
 * Verifies the JsonResultExporter's file shape and lossless round-trip through PhotonLib's packet
 * codec. Uses the package-private clock-injection constructor so the test doesn't depend on
 * NetworkTablesJNI or wall time.
 */
public class JsonResultExporterTest {
    private static final long FIXED_NOW_MICROS = 999_888_777L;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void init() {
        // AprilTagPipelineSettings transitively touches TargetModel.<clinit>, which needs OpenCV.
        if (!LibraryLoader.loadWpiLibraries()) fail("loadWpiLibraries() returned false");
    }

    private static CVPipelineResult cvResult(long seq, long captureNs) {
        return cvResult(seq, captureNs, Optional.empty());
    }

    private static CVPipelineResult cvResult(
            long seq, long captureNs, Optional<MultiTargetPNPResult> multitag) {
        var result = new CVPipelineResult(seq, 1_000_000.0, 30.0, List.of(), multitag);
        result.setImageCaptureTimestampNanos(captureNs);
        return result;
    }

    private static PhotonPipelineResult decodePacketLine(JsonNode line) {
        byte[] bytes = Base64.getDecoder().decode(line.get("packet_b64").asText());
        return PhotonPipelineResult.photonStruct.unpack(new Packet(bytes));
    }

    @Test
    public void headerEncodesPipelineHashAndTssNulls(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("cafefeed.jsonl");
        var settings = new AprilTagPipelineSettings();

        new JsonResultExporter(
                        out, "test-cam", "rec-1", settings, OffsetSnapshot.UNKNOWN, () -> FIXED_NOW_MICROS)
                .close();

        List<String> lines = Files.readAllLines(out);
        assertEquals(1, lines.size(), "should have header line only");
        JsonNode header = JSON.readTree(lines.get(0));

        assertEquals(1, header.get("schema_version").asInt());
        assertEquals("test-cam", header.get("camera_unique_name").asText());
        assertEquals("rec-1", header.get("recording_name").asText());
        assertEquals(settings.pipelineType.name(), header.get("pipeline_type").asText());
        assertEquals(Integer.toHexString(settings.hashCode()), header.get("pipeline_hash").asText());
        assertTrue(header.get("tss_active_at_record").isNull());
        assertTrue(header.get("tss_offset_at_record_ns").isNull());
    }

    @Test
    public void headerEncodesTssOffsetWhenKnown(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("a.jsonl");
        var snap = new OffsetSnapshot(true, 5_000_000L);

        new JsonResultExporter(
                        out, "cam", "rec", new AprilTagPipelineSettings(), snap, () -> FIXED_NOW_MICROS)
                .close();

        JsonNode header = JSON.readTree(Files.readAllLines(out).get(0));
        assertTrue(header.get("tss_active_at_record").asBoolean());
        assertEquals(5_000_000L, header.get("tss_offset_at_record_ns").asLong());
    }

    @Test
    public void roundTripsEmptyResult(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("a.jsonl");
        long captureNs = 123_000_000_000L;
        long seq = 42L;

        try (var exporter =
                new JsonResultExporter(
                        out,
                        "cam",
                        "rec",
                        new AprilTagPipelineSettings(),
                        OffsetSnapshot.UNKNOWN,
                        () -> FIXED_NOW_MICROS)) {
            exporter.accept(cvResult(seq, captureNs));
        }

        List<String> lines = Files.readAllLines(out);
        assertEquals(2, lines.size());
        JsonNode line = JSON.readTree(lines.get(1));
        assertEquals(captureNs, line.get("capture_ns").asLong());
        assertEquals(seq, line.get("seq").asLong());

        PhotonPipelineResult roundTripped = decodePacketLine(line);
        // UNKNOWN snapshot => offset is 0, so packet captureMicros == captureNs/1000.
        assertEquals(seq, roundTripped.metadata.sequenceID);
        assertEquals(123_000_000L, roundTripped.metadata.captureTimestampMicros);
        assertEquals(FIXED_NOW_MICROS, roundTripped.metadata.publishTimestampMicros);
        assertFalse(roundTripped.hasTargets());
        assertTrue(roundTripped.multitagResult.isEmpty());
    }

    @Test
    public void roundTripsResultWithMultitag(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("a.jsonl");
        var multitag =
                Optional.of(
                        new MultiTargetPNPResult(
                                new PnpResult(
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(0.1, 0.2, 0.3)),
                                        0.05),
                                List.of((short) 1, (short) 2, (short) 3)));

        try (var exporter =
                new JsonResultExporter(
                        out,
                        "cam",
                        "rec",
                        new AprilTagPipelineSettings(),
                        OffsetSnapshot.UNKNOWN,
                        () -> FIXED_NOW_MICROS)) {
            exporter.accept(cvResult(7L, 50_000_000_000L, multitag));
        }

        JsonNode line = JSON.readTree(Files.readAllLines(out).get(1));
        PhotonPipelineResult rt = decodePacketLine(line);
        assertTrue(rt.multitagResult.isPresent());
        assertEquals(multitag.get(), rt.multitagResult.get());
    }

    @Test
    public void appliesRecordTimeOffsetToPacketTimestamps(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("a.jsonl");
        long captureNs = 200_000_000_000L; // 200_000_000 µs
        long offsetNs = 4_000_000L; //   4_000 µs offset
        var snap = new OffsetSnapshot(true, offsetNs);

        try (var exporter =
                new JsonResultExporter(
                        out, "cam", "rec", new AprilTagPipelineSettings(), snap, () -> FIXED_NOW_MICROS)) {
            exporter.accept(cvResult(1L, captureNs));
        }

        JsonNode line = JSON.readTree(Files.readAllLines(out).get(1));
        // Outer capture_ns field is the raw recorded value (unchanged).
        assertEquals(captureNs, line.get("capture_ns").asLong());

        // Embedded packet timestamps ARE shifted by the recorded offset.
        PhotonPipelineResult rt = decodePacketLine(line);
        assertEquals(200_000_000L + 4_000L, rt.metadata.captureTimestampMicros);
        assertEquals(FIXED_NOW_MICROS + 4_000L, rt.metadata.publishTimestampMicros);
    }

    @Test
    public void differentSettingsHashesProduceDifferentFiles(@TempDir Path tempDir) throws Exception {
        var settingsA = new AprilTagPipelineSettings();
        var settingsB = new AprilTagPipelineSettings();
        settingsB.decimate = settingsA.decimate + 1; // differentiate hash
        assertNotEquals(settingsA.hashCode(), settingsB.hashCode());

        Path resultsDir = tempDir.resolve("results");
        Path fileA = resultsDir.resolve(Integer.toHexString(settingsA.hashCode()) + ".jsonl");
        Path fileB = resultsDir.resolve(Integer.toHexString(settingsB.hashCode()) + ".jsonl");
        assertNotEquals(fileA, fileB);

        try (var a =
                        new JsonResultExporter(
                                fileA, "cam", "rec", settingsA, OffsetSnapshot.UNKNOWN, () -> FIXED_NOW_MICROS);
                var b =
                        new JsonResultExporter(
                                fileB, "cam", "rec", settingsB, OffsetSnapshot.UNKNOWN, () -> FIXED_NOW_MICROS)) {
            a.accept(cvResult(0L, 1_000_000L));
            b.accept(cvResult(0L, 2_000_000L));
        }

        // Each file got exactly the result it received (no cross-contamination).
        assertEquals(
                1_000_000L,
                JSON.readTree(Files.readAllLines(fileA).get(1)).get("capture_ns").asLong());
        assertEquals(
                2_000_000L,
                JSON.readTree(Files.readAllLines(fileB).get(1)).get("capture_ns").asLong());
    }

    @Test
    public void closeFlushesAndIsIdempotent(@TempDir Path tempDir) throws Exception {
        Path out = tempDir.resolve("results").resolve("a.jsonl");
        var exporter =
                new JsonResultExporter(
                        out,
                        "cam",
                        "rec",
                        new AprilTagPipelineSettings(),
                        OffsetSnapshot.UNKNOWN,
                        () -> FIXED_NOW_MICROS);
        for (int i = 0; i < 10; i++) {
            exporter.accept(cvResult(i, 1_000_000L + i));
        }
        exporter.close();
        exporter.close(); // idempotent — no second flush, no exception

        List<String> lines = Files.readAllLines(out);
        assertEquals(11, lines.size(), "header + 10 results");
        // No partial last line.
        for (String line : lines) {
            assertNull(JSON.readTree(line).get("nonexistent"));
            assertFalse(line.isEmpty());
        }
        // accept() after close is a no-op (no IOException, no new line).
        exporter.accept(cvResult(99, 9_000_000L));
        assertEquals(11, Files.readAllLines(out).size());
    }
}

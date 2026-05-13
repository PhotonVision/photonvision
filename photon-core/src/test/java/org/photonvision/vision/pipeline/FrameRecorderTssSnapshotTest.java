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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.pipeline.FrameRecorder.TssSample;
import org.photonvision.vision.pipeline.JsonResultExporter.OffsetSnapshot;

/**
 * Verifies the tss.json hand-off between FrameRecorder (writer) and JsonResultExporter
 * (reader). The two sides only share a JSON key contract — no shared Java type — so this test
 * validates both ends round-trip cleanly.
 */
public class FrameRecorderTssSnapshotTest {
    @BeforeAll
    static void init() {
        // FrameRecorder ctor allocates a MatOfInt → OpenCV native dep, same as the integration
        // test in this package.
        if (!LibraryLoader.loadWpiLibraries()) fail("loadWpiLibraries() returned false");
    }

    @Test
    public void writesActiveSnapshot(@TempDir Path tempDir) throws Exception {
        Path outDir = tempDir.resolve("rec");
        Files.createDirectories(outDir);
        var sample = new TssSample(true, 4_500_000L, 123_456_789_000L);

        var recorder =
                new FrameRecorder(outDir, FrameRecorder.RecordingStrategy.VIDEO, Long.MAX_VALUE, sample);
        try {
            // Snapshot is written in the constructor — no need to startRecording / record frames.
        } finally {
            recorder.release();
        }

        Path tssPath = outDir.resolve("tss.json");
        assertTrue(Files.exists(tssPath), "tss.json should exist after ctor");
        JsonNode node = new ObjectMapper().readTree(tssPath.toFile());
        assertTrue(node.get("tss_active_at_record").asBoolean());
        assertEquals(4_500_000L, node.get("tss_offset_at_record_ns").asLong());
        assertEquals(123_456_789_000L, node.get("sampled_at_wpi_nt_now_ns").asLong());
    }

    @Test
    public void writesInactiveSentinel(@TempDir Path tempDir) throws Exception {
        Path outDir = tempDir.resolve("rec");
        Files.createDirectories(outDir);

        var recorder =
                new FrameRecorder(outDir, FrameRecorder.RecordingStrategy.VIDEO, Long.MAX_VALUE);
        try {
            // 3-arg ctor defaults to INACTIVE (test contract).
        } finally {
            recorder.release();
        }

        JsonNode node = new ObjectMapper().readTree(outDir.resolve("tss.json").toFile());
        assertFalse(node.get("tss_active_at_record").asBoolean());
        assertEquals(0L, node.get("tss_offset_at_record_ns").asLong());
    }

    @Test
    public void readSnapshotReturnsPopulatedFromWrittenFile(@TempDir Path tempDir) throws Exception {
        Path outDir = tempDir.resolve("rec");
        Files.createDirectories(outDir);
        var sample = new TssSample(true, 7_200_000L, 999_999L);

        var recorder =
                new FrameRecorder(outDir, FrameRecorder.RecordingStrategy.VIDEO, Long.MAX_VALUE, sample);
        recorder.release();

        OffsetSnapshot snap = JsonResultExporter.readSnapshot(outDir);
        assertNotNull(snap.tssActiveAtRecord());
        assertTrue(snap.tssActiveAtRecord());
        assertEquals(7_200_000L, snap.tssOffsetAtRecordNs());
    }

    @Test
    public void readSnapshotReturnsUnknownWhenFileMissing(@TempDir Path tempDir) {
        // tempDir has no tss.json.
        OffsetSnapshot snap = JsonResultExporter.readSnapshot(tempDir);
        assertNull(snap.tssActiveAtRecord());
        assertNull(snap.tssOffsetAtRecordNs());
    }

    @Test
    public void readSnapshotReturnsUnknownOnMalformedJson(@TempDir Path tempDir) throws Exception {
        Files.write(
                tempDir.resolve("tss.json"),
                "{not valid json".getBytes(StandardCharsets.UTF_8));

        OffsetSnapshot snap = JsonResultExporter.readSnapshot(tempDir);
        assertNull(snap.tssActiveAtRecord());
    }

    @Test
    public void readSnapshotReturnsUnknownOnMissingKeys(@TempDir Path tempDir) throws Exception {
        Files.write(
                tempDir.resolve("tss.json"),
                "{\"some_other_field\":42}".getBytes(StandardCharsets.UTF_8));

        OffsetSnapshot snap = JsonResultExporter.readSnapshot(tempDir);
        assertNull(snap.tssActiveAtRecord());
    }
}

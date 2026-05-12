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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.function.LongSupplier;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.wpilib.networktables.NetworkTablesJNI;

/**
 * Streams pipeline results to an ND-JSON file for offline consumption (e.g. AdvantageKit replay).
 *
 * <p>File shape: one header line, then one line per pipeline result. Each result line carries the
 * raw recorded capture_ns (verbatim from {@code metadata.jsonl}) plus a base64-encoded PhotonLib
 * {@link Packet} for lossless round-trip via {@code PhotonPipelineResult.photonStruct.unpack()}.
 *
 * <p>Timestamps inside the embedded packet are shifted by {@code tssOffsetAtRecordNs} so the
 * deserialized result matches what {@code NTDataPublisher} would have published live during the
 * original recording. The header records the same offset so consumers that prefer the raw
 * {@code capture_ns} field can apply it themselves.
 *
 * <p>Not thread-safe. Exactly one VisionRunner thread should call {@link #accept(CVPipelineResult)}.
 */
public class JsonResultExporter implements CVPipelineResultConsumer, AutoCloseable {
    private static final int SCHEMA_VERSION = 1;
    private static final int INITIAL_PACKET_BYTES = 1024;

    /**
     * TSS state sampled at recording-start. Null fields mean "unknown" (pre-snapshot recording);
     * the exporter then writes nulls in the header and skips the offset shift on the embedded
     * packet.
     */
    public record OffsetSnapshot(Boolean tssActiveAtRecord, Long tssOffsetAtRecordNs) {
        public static final OffsetSnapshot UNKNOWN = new OffsetSnapshot(null, null);
    }

    private final Logger logger;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OffsetSnapshot offsetSnapshot;
    private final long offsetUs;
    private final LongSupplier nowMicrosSupplier;
    private BufferedWriter writer;
    private boolean closed;

    public JsonResultExporter(
            Path outputFile,
            String cameraUniqueName,
            String recordingName,
            CVPipelineSettings settings,
            OffsetSnapshot offsetSnapshot)
            throws IOException {
        this(outputFile, cameraUniqueName, recordingName, settings, offsetSnapshot, NetworkTablesJNI::now);
    }

    JsonResultExporter(
            Path outputFile,
            String cameraUniqueName,
            String recordingName,
            CVPipelineSettings settings,
            OffsetSnapshot offsetSnapshot,
            LongSupplier nowMicrosSupplier)
            throws IOException {
        this.logger = new Logger(JsonResultExporter.class, LogGroup.VisionModule);
        this.offsetSnapshot = offsetSnapshot;
        this.nowMicrosSupplier = nowMicrosSupplier;
        this.offsetUs =
                offsetSnapshot.tssOffsetAtRecordNs() == null
                        ? 0L
                        : offsetSnapshot.tssOffsetAtRecordNs() / 1000L;

        Files.createDirectories(outputFile.getParent());
        // TRUNCATE_EXISTING + flush-per-line mirrors FrameRecorder's metadata.jsonl: each replay
        // session overwrites a stale file rather than appending malformed multi-header content.
        this.writer =
                Files.newBufferedWriter(
                        outputFile,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

        writeHeader(cameraUniqueName, recordingName, settings);
    }

    private void writeHeader(
            String cameraUniqueName, String recordingName, CVPipelineSettings settings)
            throws IOException {
        ObjectNode header = mapper.createObjectNode();
        header.put("schema_version", SCHEMA_VERSION);
        header.put("camera_unique_name", cameraUniqueName);
        header.put("recording_name", recordingName);
        header.put("pipeline_type", settings.pipelineType.name());
        header.put("pipeline_hash", Integer.toHexString(settings.hashCode()));
        if (offsetSnapshot.tssActiveAtRecord() == null) {
            header.putNull("tss_active_at_record");
        } else {
            header.put("tss_active_at_record", offsetSnapshot.tssActiveAtRecord());
        }
        if (offsetSnapshot.tssOffsetAtRecordNs() == null) {
            header.putNull("tss_offset_at_record_ns");
        } else {
            header.put("tss_offset_at_record_ns", offsetSnapshot.tssOffsetAtRecordNs());
        }
        writer.write(mapper.writeValueAsString(header));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void accept(CVPipelineResult result) {
        if (closed) return;
        try {
            // Mirror NTDataPublisher.accept (NTDataPublisher.java:200-215): build a wire-format
            // PhotonPipelineResult by adding the TSS offset to the local capture / publish
            // timestamps. We use the *record-time* offset so the deserialized result matches what
            // AKit captured live during the original match. timeSinceLastPong is not preserved
            // per-frame; emit 0.
            long captureMicros = MathUtils.nanosToMicros(result.getImageCaptureTimestampNanos());
            long nowMicros = nowMicrosSupplier.getAsLong();

            var photonResult =
                    new PhotonPipelineResult(
                            result.sequenceID,
                            captureMicros + offsetUs,
                            nowMicros + offsetUs,
                            0L,
                            TrackedTarget.simpleFromTrackedTargets(result.targets),
                            result.multiTagResult);

            Packet packet = new Packet(INITIAL_PACKET_BYTES);
            PhotonPipelineResult.photonStruct.pack(packet, photonResult);
            String b64 = Base64.getEncoder().encodeToString(packet.getWrittenDataCopy());

            ObjectNode line = mapper.createObjectNode();
            line.put("capture_ns", result.getImageCaptureTimestampNanos());
            line.put("seq", result.sequenceID);
            line.put("packet_b64", b64);
            writer.write(mapper.writeValueAsString(line));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Failed to write json result line: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close JsonResultExporter: " + e.getMessage());
        }
    }
}

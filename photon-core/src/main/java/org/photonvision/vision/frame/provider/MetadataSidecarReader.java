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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Streams {@code (seq, capture_ns)} pairs out of a {@code metadata.jsonl} sidecar written by {@code
 * FrameRecorder}.
 *
 * <p>Schema invariants this reader relies on (mirroring the writer's contract):
 *
 * <ul>
 *   <li>One JSON object per line, e.g. {@code {"seq":N,"capture_ns":T}}.
 *   <li>Unknown fields are silently ignored — the schema is forward-compatible, so future additions
 *       (exposure, gain, calibration version) won't break old readers.
 *   <li>The writer flushes the metadata line <em>before</em> writing its paired JPEG frame, so
 *       {@code line_count(jsonl) >= frame_count(frames/)} holds under any crash. Callers that
 *       consume from both in lockstep should stop pulling lines once the frame directory is
 *       exhausted — never the other way around.
 * </ul>
 *
 * <p>Missing/malformed fields on a non-empty line are fatal ({@link IOException} naming the line
 * number). Truncated trailing partial lines are likewise fatal; in practice the lockstep-with-
 * frames pattern above means the partial line is never reached.
 */
public class MetadataSidecarReader implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BufferedReader reader;
    private long lineNumber = 0;

    /** One (seq, capture_ns) pair from the sidecar. */
    public record Entry(long seq, long captureNs) {}

    public MetadataSidecarReader(Path jsonlPath) throws IOException {
        this.reader = Files.newBufferedReader(jsonlPath, StandardCharsets.UTF_8);
    }

    /**
     * Reads the next entry from the sidecar.
     *
     * @return the parsed entry, or {@link Optional#empty()} at end of file.
     * @throws IOException on read failure, malformed JSON, or missing required fields. The exception
     *     message includes the 1-based line number for diagnosis.
     */
    public Optional<Entry> readNext() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return Optional.empty();
        }
        lineNumber++;

        JsonNode node;
        try {
            node = MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IOException("metadata.jsonl line " + lineNumber + " is not valid JSON: " + line, e);
        }

        JsonNode seqNode = node.get("seq");
        JsonNode captureNode = node.get("capture_ns");
        if (seqNode == null || !seqNode.canConvertToLong()) {
            throw new IOException(
                    "metadata.jsonl line " + lineNumber + " missing or non-numeric 'seq': " + line);
        }
        if (captureNode == null || !captureNode.canConvertToLong()) {
            throw new IOException(
                    "metadata.jsonl line " + lineNumber + " missing or non-numeric 'capture_ns': " + line);
        }

        return Optional.of(new Entry(seqNode.asLong(), captureNode.asLong()));
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}

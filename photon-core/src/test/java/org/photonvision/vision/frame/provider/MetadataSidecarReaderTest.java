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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link MetadataSidecarReader}. Pure JVM — no OpenCV, no JNI, runs anywhere.
 *
 * <p>The fixture format mirrors what {@code FrameRecorder.writeMetadataLine} writes: {@code
 * "{\"seq\":N,\"capture_ns\":T}\n"} — note the trailing newline on every line including the last,
 * so a sane file ends with an empty segment after the final {@code \n}.
 */
class MetadataSidecarReaderTest {
    @TempDir Path tempDir;

    private Path write(String contents) throws IOException {
        Path p = tempDir.resolve("metadata.jsonl");
        Files.writeString(p, contents, StandardCharsets.UTF_8);
        return p;
    }

    @Test
    void readsKnownFields() throws IOException {
        Path p = write("{\"seq\":0,\"capture_ns\":1000}\n" + "{\"seq\":1,\"capture_ns\":1033333}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            var first = reader.readNext();
            assertTrue(first.isPresent());
            assertEquals(0L, first.get().seq());
            assertEquals(1000L, first.get().captureNs());

            var second = reader.readNext();
            assertTrue(second.isPresent());
            assertEquals(1L, second.get().seq());
            assertEquals(1033333L, second.get().captureNs());

            assertEquals(Optional.empty(), reader.readNext());
        }
    }

    @Test
    void ignoresUnknownFields() throws IOException {
        // Schema is forward-compatible: future writers may add exposure_us, gain_db, calibration
        // version. Reader must ignore them and still produce the correct seq/capture_ns.
        Path p =
                write(
                        "{\"seq\":5,\"capture_ns\":42,\"exposure_us\":1200,"
                                + "\"gain_db\":3.5,\"calibration_version\":\"v2\"}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            var entry = reader.readNext();
            assertTrue(entry.isPresent());
            assertEquals(5L, entry.get().seq());
            assertEquals(42L, entry.get().captureNs());
        }
    }

    @Test
    void handlesTrailingNewline() throws IOException {
        // FrameRecorder always writes "...}\n" so the file ends with an empty segment after the
        // final newline. That must read as EOF, not produce an error.
        Path p = write("{\"seq\":0,\"capture_ns\":1}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            assertTrue(reader.readNext().isPresent());
            assertEquals(Optional.empty(), reader.readNext());
        }
    }

    @Test
    void emptyFileIsEof() throws IOException {
        Path p = write("");
        try (var reader = new MetadataSidecarReader(p)) {
            assertEquals(Optional.empty(), reader.readNext());
        }
    }

    @Test
    void failsOnMissingFields() throws IOException {
        try (var reader = new MetadataSidecarReader(write("{\"capture_ns\":123}\n"))) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1") && ex.getMessage().contains("seq"));
        }
        try (var reader = new MetadataSidecarReader(write("{\"seq\":1}\n"))) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1") && ex.getMessage().contains("capture_ns"));
        }
    }

    @Test
    void failsOnMalformedJson() throws IOException {
        try (var reader = new MetadataSidecarReader(write("{seq:1,capture_ns:2\n"))) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1"));
            assertTrue(ex.getMessage().toLowerCase().contains("json"));
        }
    }

    @Test
    void missingFileFailsAtConstruction() {
        assertThrows(
                IOException.class,
                () -> new MetadataSidecarReader(tempDir.resolve("does-not-exist.jsonl")));
    }
}

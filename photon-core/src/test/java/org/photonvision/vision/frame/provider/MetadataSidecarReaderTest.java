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
    void failsOnMissingSeq() throws IOException {
        Path p = write("{\"capture_ns\":123}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1"));
            assertTrue(ex.getMessage().contains("seq"));
        }
    }

    @Test
    void failsOnMissingCaptureNs() throws IOException {
        Path p = write("{\"seq\":1}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1"));
            assertTrue(ex.getMessage().contains("capture_ns"));
        }
    }

    @Test
    void failsOnNonNumericField() throws IOException {
        Path p = write("{\"seq\":\"abc\",\"capture_ns\":123}\n");
        try (var reader = new MetadataSidecarReader(p)) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1"));
            assertTrue(ex.getMessage().contains("seq"));
        }
    }

    @Test
    void failsOnMalformedJson() throws IOException {
        Path p = write("{seq:1,capture_ns:2\n");
        try (var reader = new MetadataSidecarReader(p)) {
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 1"));
            assertTrue(ex.getMessage().toLowerCase().contains("json"));
        }
    }

    @Test
    void failsOnShortTrailingLine() throws IOException {
        // Simulates writer crash mid-line. The lockstep-with-frames pattern in the provider stops
        // before we ever read this line; if a caller does reach it, fail loudly with a line
        // number so they can diagnose.
        Path p = write("{\"seq\":0,\"capture_ns\":1000}\n" + "{\"seq\":1,\"capture_n");
        try (var reader = new MetadataSidecarReader(p)) {
            assertTrue(reader.readNext().isPresent());
            IOException ex = assertThrows(IOException.class, reader::readNext);
            assertTrue(ex.getMessage().contains("line 2"));
        }
    }

    @Test
    void missingFileFailsAtConstruction() {
        Path p = tempDir.resolve("does-not-exist.jsonl");
        assertThrows(IOException.class, () -> new MetadataSidecarReader(p));
    }

    @Test
    void closeReleasesFileHandle() throws IOException {
        Path p = write("{\"seq\":0,\"capture_ns\":1}\n");
        var reader = new MetadataSidecarReader(p);
        reader.close();
        // After close, deleting the file must succeed even on Windows (which holds open-file
        // locks). If close didn't release the handle, Files.delete would throw.
        assertTrue(Files.deleteIfExists(p));
        assertFalse(Files.exists(p));
    }
}

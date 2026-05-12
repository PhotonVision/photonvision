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

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.PVCameraInfo;

/**
 * Unit tests for {@link VisionSourceManager#enumerateRecordedSources(Path,
 * java.util.function.Consumer)}. Pure JVM — no JNI, no HAL, no ConfigManager singleton.
 *
 * <p>The full {@link VisionSourceManagerTest} requires {@code photontargetingJNI} and so can't
 * run on dev machines without Visual Studio. This test exercises the recording-enumeration
 * helper in isolation, so it's portable.
 */
class VisionSourceManagerEnumerationTest {
    @TempDir Path recordingsRoot;

    private void touch(Path p) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, "x", StandardCharsets.UTF_8);
    }

    private Path makeValidRecording(String camera, String recording) throws IOException {
        Path dir = recordingsRoot.resolve(camera).resolve(recording);
        Files.createDirectories(dir.resolve("frames"));
        touch(dir.resolve("metadata.jsonl"));
        return dir;
    }

    @Test
    void emptyRootProducesNothing() {
        List<String> warns = new ArrayList<>();
        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, warns::add);
        assertTrue(result.isEmpty());
        assertTrue(warns.isEmpty(), "no warns for an empty but extant directory");
    }

    @Test
    void missingRootIsSilent() {
        List<String> warns = new ArrayList<>();
        var result =
                VisionSourceManager.enumerateRecordedSources(
                        recordingsRoot.resolve("nope"), warns::add);
        assertTrue(result.isEmpty());
        assertTrue(warns.isEmpty(), "non-existent recordings root is normal on a fresh install");
    }

    @Test
    void surfacesCompleteRecording() throws IOException {
        Path rec = makeValidRecording("camA", "session-1");
        List<String> warns = new ArrayList<>();
        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, warns::add);
        assertTrue(warns.isEmpty());
        assertEquals(1, result.size());
        PVCameraInfo info = result.get(0);
        assertEquals(CameraType.FileLogCamera, info.type());
        assertEquals(rec.toString(), info.path());
        assertEquals("camA/session-1", info.name());
        assertEquals(info.path(), info.uniquePath(), "uniquePath should equal path for dedup");
    }

    @Test
    void skipsRecordingMissingMetadata() throws IOException {
        // frames/ present but no jsonl — could be a pre-2183 recording, could be mid-init.
        // Either way the provider would refuse to construct, so don't offer it as a camera.
        Path dir = recordingsRoot.resolve("camA").resolve("pre-2183");
        Files.createDirectories(dir.resolve("frames"));

        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        assertTrue(result.isEmpty());
    }

    @Test
    void skipsRecordingMissingFrames() throws IOException {
        // jsonl present but no frames/ — recorder probably opened metadata then crashed before
        // writing any frames. Provider refuses construction; don't offer it.
        Path dir = recordingsRoot.resolve("camA").resolve("metadata-only");
        Files.createDirectories(dir);
        touch(dir.resolve("metadata.jsonl"));

        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        assertTrue(result.isEmpty());
    }

    @Test
    void emitsOneEntryPerLeafAcrossCameras() throws IOException {
        makeValidRecording("camA", "morning");
        makeValidRecording("camA", "afternoon");
        makeValidRecording("camB", "match-1");

        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        assertEquals(3, result.size());
        var names = result.stream().map(PVCameraInfo::name).collect(Collectors.toSet());
        assertTrue(names.contains("camA/morning"));
        assertTrue(names.contains("camA/afternoon"));
        assertTrue(names.contains("camB/match-1"));
    }

    @Test
    void ignoresLooseFilesAtCameraLevel() throws IOException {
        // recordings/camA/strat (a file, not a dir) shouldn't trip up the walker.
        Files.createDirectories(recordingsRoot.resolve("camA"));
        touch(recordingsRoot.resolve("camA").resolve("strat"));
        makeValidRecording("camA", "real-rec");

        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        assertEquals(1, result.size());
        assertEquals("camA/real-rec", result.get(0).name());
    }

    @Test
    void ignoresLooseFilesAtRoot() throws IOException {
        // someone dropped a random file in the recordings root
        touch(recordingsRoot.resolve("readme.txt"));
        makeValidRecording("camA", "rec1");

        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        assertEquals(1, result.size());
    }

    @Test
    void filesInsteadOfRootIsSilent() throws IOException {
        // If the user has somehow pointed recordingsDir at a file, treat it like a missing
        // directory — no recordings, no warn. ConfigManager normally creates the directory,
        // but defending here keeps the call site simple.
        Path file = recordingsRoot.resolve("not-a-dir");
        touch(file);
        List<String> warns = new ArrayList<>();
        var result = VisionSourceManager.enumerateRecordedSources(file, warns::add);
        assertTrue(result.isEmpty());
        assertTrue(warns.isEmpty());
    }

    @Test
    void uniquePathIsAbsoluteAndUsableByProvider() throws IOException {
        // The PVFileLogCameraInfo.uniquePath() is what VisionSourceManager later passes to
        // FileLogFrameProvider.<init>(Path.of(...)) — make sure it's an absolute path that round
        // trips through Path.of so the provider can actually open the recording.
        Path rec = makeValidRecording("camA", "rec1");
        var result = VisionSourceManager.enumerateRecordedSources(recordingsRoot, msg -> {});
        Path roundTripped = Path.of(result.get(0).uniquePath());
        assertEquals(rec.toAbsolutePath().normalize(), roundTripped.toAbsolutePath().normalize());
        assertFalse(result.get(0).uniquePath().isEmpty());
    }
}

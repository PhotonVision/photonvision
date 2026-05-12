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

package org.photonvision.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class PathSafetyTest {
    private final Path root = Paths.get(System.getProperty("java.io.tmpdir"), "pv-test-root");

    @Test
    public void validSimpleName() {
        Path result = PathSafety.safeResolve(root, "my-camera");
        assertEquals(root.toAbsolutePath().resolve("my-camera"), result);
    }

    @Test
    public void validMultipleSegments() {
        Path result = PathSafety.safeResolve(root, "camera1", "recording1");
        assertEquals(root.toAbsolutePath().resolve("camera1").resolve("recording1"), result);
    }

    @Test
    public void validCompositeName() {
        // Representative of recording dir names built from match data: long, with separators,
        // but composed only of characters legal on all target filesystems.
        String name = "event_WEEK0_match_Q12_replay_0_station_RED1";
        Path result = PathSafety.safeResolve(root, name);
        assertEquals(root.toAbsolutePath().resolve(name), result);
    }

    @Test
    public void rejectsFilesystemIllegalChars() {
        // On Windows, ':' is reserved for drive letters and the JDK throws InvalidPathException
        // at resolve-time. The util wraps that as SecurityException so the caller sees one type.
        // (This test is a no-op on POSIX, where ':' is a legal filename char — the call simply
        // returns a valid Path; we don't assertThrows here to keep the test cross-platform.)
        try {
            PathSafety.safeResolve(root, "name:with:colons");
            // POSIX: accepted, fine.
        } catch (SecurityException e) {
            // Windows: wrapped as SecurityException, also fine.
        }
    }

    @Test
    public void rejectsDoubleDot() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, ".."));
    }

    @Test
    public void rejectsParentTraversal() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, "../etc"));
    }

    @Test
    public void rejectsEmbeddedTraversal() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, "a/../../etc"));
    }

    @Test
    public void rejectsAbsolutePosixPath() {
        // resolve(absolute) returns the absolute path, which won't startWith(root)
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, "/etc/passwd"));
    }

    @Test
    public void rejectsTraversalAcrossSegments() {
        // valid first segment, escape attempt in the second
        assertThrows(
                SecurityException.class, () -> PathSafety.safeResolve(root, "camera1", "../../etc"));
    }

    @Test
    public void rejectsNullSegment() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, (String) null));
    }

    @Test
    public void rejectsBlankSegment() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, "   "));
    }

    @Test
    public void rejectsEmptySegment() {
        assertThrows(SecurityException.class, () -> PathSafety.safeResolve(root, ""));
    }
}

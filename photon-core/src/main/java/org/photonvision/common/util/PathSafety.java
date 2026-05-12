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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

/** Helpers for resolving user-supplied path segments without allowing escape from a root. */
public final class PathSafety {
    private PathSafety() {}

    /**
     * Resolve user-supplied segments inside a trusted root, rejecting any input that would escape
     * the root (e.g. {@code ".."}, absolute paths, embedded {@code "../"} sequences) or that the
     * underlying filesystem refuses to parse.
     *
     * @param root the trusted root directory. The result is guaranteed to be inside this directory.
     * @param segments user-supplied path segments, applied in order via {@link Path#resolve(String)}.
     * @return the resolved, normalized, absolute path. Always inside {@code root}.
     * @throws SecurityException if any segment is null/blank, contains characters the filesystem
     *     can't parse, or if the resolved path is outside the root.
     */
    public static Path safeResolve(Path root, String... segments) {
        Path rootAbs = root.toAbsolutePath().normalize();
        Path resolved = rootAbs;
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                throw new SecurityException("Empty path segment");
            }
            try {
                resolved = resolved.resolve(segment);
            } catch (InvalidPathException e) {
                throw new SecurityException("Invalid path segment: " + segment, e);
            }
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(rootAbs)) {
            throw new SecurityException("Path escapes root: " + Arrays.toString(segments));
        }
        return resolved;
    }
}

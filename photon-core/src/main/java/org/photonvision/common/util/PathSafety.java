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
     * Resolve user-supplied segments inside a trusted root, rejecting any input that would escape the
     * root or widen scope beyond the directory each segment names. Every segment must be a single
     * path name: {@code "."}, {@code ".."}, absolute or drive-relative paths (e.g. {@code "C:x"} on
     * Windows), segments containing {@code "/"} or {@code "\"}, and anything the underlying
     * filesystem refuses to parse are all rejected.
     *
     * @param root the trusted root directory. The result is guaranteed to be strictly inside this
     *     directory (never the root itself).
     * @param segments user-supplied path segments, applied in order via {@link Path#resolve(Path)}.
     * @return the resolved, normalized, absolute path. Always strictly inside {@code root}.
     * @throws SecurityException if any segment is null/blank, is not a single path name, contains
     *     characters the filesystem can't parse, or if the resolved path is not strictly inside the
     *     root.
     */
    public static Path safeResolve(Path root, String... segments) {
        Path rootAbs = root.toAbsolutePath().normalize();
        Path resolved = rootAbs;
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                throw new SecurityException("Empty path segment");
            }
            if (segment.equals(".")
                    || segment.equals("..")
                    || segment.indexOf('/') >= 0
                    || segment.indexOf('\\') >= 0) {
                throw new SecurityException("Path segment is not a single name: " + segment);
            }
            Path segmentPath;
            try {
                segmentPath = rootAbs.getFileSystem().getPath(segment);
            } catch (InvalidPathException e) {
                throw new SecurityException("Invalid path segment: " + segment, e);
            }
            if (segmentPath.isAbsolute()
                    || segmentPath.getRoot() != null
                    || segmentPath.getNameCount() != 1) {
                throw new SecurityException("Path segment is not a single name: " + segment);
            }
            resolved = resolved.resolve(segmentPath);
        }
        resolved = resolved.normalize();
        if (resolved.equals(rootAbs) || !resolved.startsWith(rootAbs)) {
            throw new SecurityException("Path escapes root: " + Arrays.toString(segments));
        }
        return resolved;
    }
}

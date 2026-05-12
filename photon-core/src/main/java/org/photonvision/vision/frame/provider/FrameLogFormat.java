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

import java.nio.file.Path;

/**
 * Single source of truth for the on-disk frame log filename pattern shared by {@code FrameRecorder}
 * and {@code FileLogFrameProvider}. Zero-padded so lexical sort matches numeric.
 */
public final class FrameLogFormat {
    private static final String FILENAME_FORMAT = "%06d.jpg";

    private FrameLogFormat() {}

    /** On-disk path of the JPEG at a given sequence number under a recording's frames dir. */
    public static Path framePath(Path framesDir, long seq) {
        return framesDir.resolve(String.format(FILENAME_FORMAT, seq));
    }
}

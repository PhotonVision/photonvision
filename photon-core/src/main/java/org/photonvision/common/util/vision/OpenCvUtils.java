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

package org.photonvision.common.util.vision;

import edu.wpi.first.cscore.VideoMode;

public class OpenCvUtils {
    private OpenCvUtils() {}

    public static boolean videoModeEquals(VideoMode a, VideoMode b) {
        // WPILib doesn't provide an equals(), so implement our own here
        if (a.pixelFormat != b.pixelFormat) return false;
        if (a.width != b.width) return false;
        if (a.height != b.height) return false;
        if (a.fps != b.fps) return false;
        return true;
    }
}

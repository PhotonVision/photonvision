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

package org.photonvision.vision.opencv;

import org.opencv.core.Core;

public enum ImageRotationMode {
    DEG_0(-1),
    DEG_270_CCW(Core.ROTATE_90_CLOCKWISE),
    DEG_180(Core.ROTATE_180),
    DEG_90_CCW(Core.ROTATE_90_COUNTERCLOCKWISE);

    public final int value;

    ImageRotationMode(int value) {
        this.value = value;
    }
}

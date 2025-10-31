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

package org.photonvision.vision.pipe.impl;

import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;

public record NeuralNetworkPipeResult(RotatedRect bbox, int classIdx, double confidence) {
    public NeuralNetworkPipeResult(Rect2d rect, int classIdx, double confidence) {
        // turn the axis-aligned rect into a RotatedRect with angle 0 degrees
        this(
                new RotatedRect(
                        new Point(rect.x + (rect.width) / 2, rect.y + (rect.height) / 2),
                        new Size(rect.width, rect.height),
                        0.0),
                classIdx,
                confidence);
    }
}

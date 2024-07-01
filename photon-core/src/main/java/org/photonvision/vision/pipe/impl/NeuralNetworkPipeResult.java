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

import org.opencv.core.Rect2d;

public class NeuralNetworkPipeResult {
    public NeuralNetworkPipeResult(Rect2d boundingBox, int classIdx, double confidence) {
        bbox = boundingBox;
        this.classIdx = classIdx;
        this.confidence = confidence;
    }

    public final int classIdx;
    public final Rect2d bbox;
    public final double confidence;
}

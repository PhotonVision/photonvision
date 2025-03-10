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

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.photonvision.vision.pipe.CVPipe;

public class UncropObjectDetectionPipe
        extends CVPipe<List<NeuralNetworkPipeResult>, List<NeuralNetworkPipeResult>, Rect> {
    public UncropObjectDetectionPipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected List<NeuralNetworkPipeResult> process(List<NeuralNetworkPipeResult> in) {
        List<NeuralNetworkPipeResult> temp = new ArrayList<>();

        for (NeuralNetworkPipeResult result : in) {
            temp.add(
                    new NeuralNetworkPipeResult(
                            offsetObjectBoundingBox(result.bbox), result.classIdx, result.confidence));
        }

        return temp;
    }

    private Rect2d offsetObjectBoundingBox(Rect2d in) {
        return new Rect2d(in.x + params.x, in.y + params.y, in.width, in.height);
    }
}

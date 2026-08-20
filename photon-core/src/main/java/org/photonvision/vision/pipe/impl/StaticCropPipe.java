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

import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;

/**
 * Crops an image to the rectangle given as the pipe's params. The output is a view into the input.
 */
public class StaticCropPipe extends CVPipe<CVMat, CVMat, Rect> {
    @Override
    protected CVMat process(CVMat in) {
        if (params == null || in.getMat().empty()) {
            return null;
        }

        return new CVMat(in.getMat().submat(params));
    }
}

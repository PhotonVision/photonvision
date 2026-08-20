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

public class StaticCropPipe extends CVPipe<CVMat, CVMat, Rect> {
    private Rect cropArea = null;

    public StaticCropPipe() {
        super();
    }

    @Override
    protected CVMat process(CVMat in) {
        if (in.getMat().empty()) {
            return null;
        }

        if (cropArea == null) {
            return null;
        }

        return new CVMat(in.getMat().submat(cropArea));
    }

    @Override
    public void setParams(Rect cropArea) {
        if (this.cropArea == null || !this.cropArea.equals(cropArea)) {
            this.cropArea = cropArea;
        }

        super.setParams(cropArea);
    }
}

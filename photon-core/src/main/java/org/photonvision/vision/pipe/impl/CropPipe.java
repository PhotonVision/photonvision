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

import edu.wpi.first.math.MathUtil;
import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;

public class CropPipe extends CVPipe<CVMat, CVMat, Rect> {
    public CropPipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected CVMat process(CVMat in) {
        int x = MathUtil.clamp(params.x, 0, in.getMat().width());
        int y = MathUtil.clamp(params.y, 0, in.getMat().height());
        int width = MathUtil.clamp(params.width, 0, in.getMat().width() - x);
        int height = MathUtil.clamp(params.height, 0, in.getMat().height() - y);

        return new CVMat(in.getMat().submat(y, y + height, x, x + width));
    }

    @Override
    public void setParams(Rect params) {
        params.height = Math.max(16, params.height);
        params.width = Math.max(16, params.width);

        super.setParams(params);
    }
}
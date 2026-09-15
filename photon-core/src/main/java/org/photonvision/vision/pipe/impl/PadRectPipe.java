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
import org.photonvision.vision.pipe.CVPipe;

/**
 * Grows a rectangle by a fraction of its own size on every side. Used to pad the ML tag detector's
 * bounding boxes before they are cropped: the model boxes tags tightly, and a tag whose border
 * touches the crop edge loses its quiet zone and goes undetected.
 *
 * <p>The parameter is the padding factor: how much each side of the rectangle grows, as a fraction
 * of the rectangle's size on that axis.
 *
 * <p>The output may reach outside the frame; callers are expected to clamp it.
 */
public class PadRectPipe extends CVPipe<Rect, Rect, Double> {
    @Override
    protected Rect process(Rect in) {
        int padX = (int) Math.ceil(in.width * params);
        int padY = (int) Math.ceil(in.height * params);
        return new Rect(in.x - padX, in.y - padY, in.width + 2 * padX, in.height + 2 * padY);
    }

    @Override
    public void release() {}
}

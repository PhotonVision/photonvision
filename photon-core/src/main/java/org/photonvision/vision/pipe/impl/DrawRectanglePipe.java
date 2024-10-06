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
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.MutatingPipe;

public class DrawRectanglePipe
        extends MutatingPipe<Mat, DrawRectanglePipe.DrawRectanglePipeParams> {
    Scalar color;

    public DrawRectanglePipe(Scalar color) {
        super();
        this.params = new DrawRectanglePipeParams();
        this.color = color;
    }

    @Override
    protected Void process(Mat in) {
        // Draw nothing if the rectangle fully covers the image
        if (CropPipe.fullyCovers(params.rect, in)) {
            return null;
        }

        int x = MathUtil.clamp(params.rect.x, 0, in.width());
        int y = MathUtil.clamp(params.rect.y, 0, in.height());
        int width = MathUtil.clamp(params.rect.width, 0, in.width() - x);
        int height = MathUtil.clamp(params.rect.height, 0, in.height() - y);
        Rect rect = new Rect(x, y, width, height);

        Imgproc.rectangle(in, rect, this.color, params.thickness);
        return null;
    }

    public static class DrawRectanglePipeParams {
        public Rect rect = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        public int thickness = 2;

        public DrawRectanglePipeParams() {}

        public DrawRectanglePipeParams(Rect rect) {
            this.rect = rect;
        }

        public DrawRectanglePipeParams(int x, int y, int width, int height) {
            this(new Rect(x, y, width, height));
        }
    }
}

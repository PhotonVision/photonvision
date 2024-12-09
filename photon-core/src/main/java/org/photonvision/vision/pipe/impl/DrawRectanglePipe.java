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
    Scalar staticColor;
    Scalar dynamicColor;
    
        public DrawRectanglePipe(Scalar staticColor, Scalar dynamicColor) {
            super();
            this.params = new DrawRectanglePipeParams();
            this.staticColor = staticColor;
            this.dynamicColor = dynamicColor;
    }

    @Override
    protected Void process(Mat in) {
        // Draw nothing if the rectangle fully covers the image
        if (CropPipe.fullyCovers(params.static_rect, in)) {
            return null;
        }

        int static_x = MathUtil.clamp(params.static_rect.x, 0, in.width());
        int static_y = MathUtil.clamp(params.static_rect.y, 0, in.height());
        int static_width = MathUtil.clamp(params.static_rect.width, 0, in.width() - static_x);
        int static_height = MathUtil.clamp(params.static_rect.height, 0, in.height() - static_y);
        Rect static_rect = new Rect(static_x, static_y, static_width, static_height);

        int dynamic_x = MathUtil.clamp(params.dynamic_rect.x, 0, in.width());
        int dynamic_y = MathUtil.clamp(params.dynamic_rect.y, 0, in.height());
        int dynamic_width = MathUtil.clamp(params.dynamic_rect.width, 0, in.width() - dynamic_x);
        int dynamic_height = MathUtil.clamp(params.dynamic_rect.height, 0, in.height() - dynamic_y);
        Rect dynamic_rect = new Rect(dynamic_x, dynamic_y, dynamic_width, dynamic_height);

        Imgproc.rectangle(in, static_rect, this.staticColor, params.thickness);
        Imgproc.rectangle(in, dynamic_rect, this.dynamicColor, params.thickness);

        return null;
    }

    public static class DrawRectanglePipeParams {
        public Rect static_rect = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        public Rect dynamic_rect = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        public int thickness = 2;

        public DrawRectanglePipeParams() {}

        public DrawRectanglePipeParams(Rect static_rect,Rect dynamic_rect) {
            this.static_rect = static_rect;
            this.dynamic_rect = dynamic_rect;
        }

        public DrawRectanglePipeParams(int x, int y, int width, int height, int x2, int y2, int width2, int height2) {
            this(new Rect(x, y, width, height),new Rect(x2, y2, width2, height2));
        }
    }
}

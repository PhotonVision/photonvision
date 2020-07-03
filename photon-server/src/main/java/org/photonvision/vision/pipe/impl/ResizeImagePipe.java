/*
 * Copyright (C) 2020 Photon Vision.
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

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.CVPipe;

/** Pipe that resizes an image to a given resolution */
public class ResizeImagePipe extends CVPipe<Mat, Mat, ResizeImagePipe.ResizeImageParams> {

    public ResizeImagePipe() {
        setParams(ResizeImageParams.DEFAULT);
    }

    public ResizeImagePipe(ResizeImageParams params) {
        setParams(params);
    }

    /**
    * Process this pipe
    *
    * @param in {@link Mat} to be resized
    * @return Resized {@link Mat}
    */
    @Override
    protected Mat process(Mat in) {

        // if a divisor is set, use that instead of a size.
        if (params.getDivisor() != null) {
            int width = in.cols() / params.getDivisor().value;
            int height = in.rows() / params.getDivisor().value;
            setParams(new ResizeImageParams(width, height));
        }

        Imgproc.resize(in, in, params.getSize());
        return in;
    }

    public static class ResizeImageParams {
        public static ResizeImageParams DEFAULT = new ResizeImageParams(320, 240);

        private Size size;
        private int width;
        private int height;
        private FrameDivisor divisor;

        public ResizeImageParams() {
            this(DEFAULT.width, DEFAULT.height);
        }

        public ResizeImageParams(int width, int height) {
            this.width = width;
            this.height = height;
            size = new Size(new double[] {width, height});
        }

        public ResizeImageParams(FrameDivisor divisor) {
            this.divisor = divisor;
        }

        public Size getSize() {
            return size;
        }

        public FrameDivisor getDivisor() {
            return divisor;
        }
    }
}

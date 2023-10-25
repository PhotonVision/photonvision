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

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.MutatingPipe;

/** Pipe that resizes an image to a given resolution */
public class ResizeImagePipe extends MutatingPipe<Mat, ResizeImagePipe.ResizeImageParams> {
    /**
     * Process this pipe
     *
     * @param in {@link Mat} to be resized
     */
    @Override
    protected Void process(Mat in) {
        int width = in.cols() / params.getDivisor().value;
        int height = in.rows() / params.getDivisor().value;
        Imgproc.resize(in, in, new Size(width, height));

        return null;
    }

    public static class ResizeImageParams {
        private final FrameDivisor divisor;

        public ResizeImageParams(FrameDivisor divisor) {
            this.divisor = divisor;
        }

        public FrameDivisor getDivisor() {
            return divisor;
        }
    }
}

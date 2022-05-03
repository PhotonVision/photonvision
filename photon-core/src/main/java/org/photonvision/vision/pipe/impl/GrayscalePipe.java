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
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

public class GrayscalePipe extends CVPipe<Mat, Mat, GrayscalePipe.GrayscaleParams> {
    @Override
    protected Mat process(Mat in) {
        var outputMat = new Mat();
        // We can save a copy here by sending the output of cvtcolor to outputMat directly
        // rather than copying. Free performance!
        Imgproc.cvtColor(in, outputMat, Imgproc.COLOR_BGR2GRAY, 3);

        return outputMat;
    }

    public static class GrayscaleParams {
        public GrayscaleParams() {}
    }
}

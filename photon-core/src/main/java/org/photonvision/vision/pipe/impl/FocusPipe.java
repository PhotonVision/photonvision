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

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

public class FocusPipe extends CVPipe<Mat, FocusPipe.FocusResult, FocusPipe.FocusParams> {
    private double maxVariance = 0.0;

    @Override
    protected FocusResult process(Mat in) {
        var outputMat = new Mat();

        Imgproc.Laplacian(in, outputMat, CvType.CV_64F, 3);

        var mean = new MatOfDouble();
        var stddev = new MatOfDouble();
        Core.meanStdDev(outputMat, mean, stddev);
        var sd = stddev.get(0, 0)[0];
        var variance = sd * sd;

        return new FocusResult(outputMat, variance);
    }

    public static class FocusResult {
        public final Mat frame;
        public final double variance;

        public FocusResult(Mat frame, double variance) {
            this.frame = frame;
            this.variance = variance;
        }
    }

    public static class FocusParams {}
}

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
import org.opencv.core.CvType;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

public class FocusPipe extends CVPipe<Mat, Mat, FocusPipe.FocusParams> {
    @Override
    protected Mat process(Mat in) {
        var outputMat = new Mat();
        // We can save a copy here by sending the output of cvtcolor to outputMat directly
        // rather than copying. Free performance!
        //Imgproc.cvtColor(in, outputMat, Imgproc.COLOR_BGR2GRAY, 3);
    // Compute the Laplacian in double precision (like cv2.CV_64F)
    Imgproc.Laplacian(in, outputMat, CvType.CV_64F, 3);

    // Compute standard deviation (and square it for variance) on the double Laplacian
    var mean = new org.opencv.core.MatOfDouble();
    var stddev = new org.opencv.core.MatOfDouble();
    Core.meanStdDev(outputMat, mean, stddev);
    var sd = stddev.get(0, 0)[0];
    var variance = sd * sd;

        

        // Draw the variance on the image (displayMat is 8-bit)
        Imgproc.putText(
            outputMat,
            String.format("%.2f", variance),
            new org.opencv.core.Point(10, 30),
            Imgproc.FONT_HERSHEY_SIMPLEX,
            0.8,
            new org.opencv.core.Scalar(255, 255, 255),
            2
        );

        return outputMat;
    }

    public static class FocusParams {}
}

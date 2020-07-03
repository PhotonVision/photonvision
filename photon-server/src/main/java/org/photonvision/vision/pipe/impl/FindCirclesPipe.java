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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.CVPipe;

public class FindCirclesPipe
        extends CVPipe<Pair<Mat, List<Contour>>, List<CVShape>, FindCirclesPipe.FindCirclePipeParams> {

    double[] c;
    Mat circles = new Mat();
    Moments mu;
    double x_center;
    double y_center;
    /**
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return Result of processing.
    */
    @Override
    protected List<CVShape> process(Pair<Mat, List<Contour>> in) {
        circles.release();
        List<CVShape> output = new ArrayList<>();

        Imgproc.HoughCircles(
                in.getLeft(),
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.0,
                params.minDist,
                params.maxCannyThresh,
                params.accuracy,
                params.minRadius,
                params.maxRadius);
        for (int x = 0; x < circles.cols(); x++) {
            c = circles.get(0, x);
            x_center = c[0];
            y_center = c[1];
            for (Contour contour : in.getRight()) {
                mu = contour.getMoments();
                if (Math.abs(x_center - (mu.m10 / mu.m00)) <= params.allowableThreshold
                        && Math.abs(y_center - (mu.m01 / mu.m00)) <= params.allowableThreshold) {
                    output.add(new CVShape(contour, ContourShape.Circle));
                }
            }
        }

        return output;
    }

    public static class FindCirclePipeParams {
        public int allowableThreshold;
        public int minRadius;
        public int maxRadius;
        public int minDist;
        public int maxCannyThresh;
        public int accuracy;

        public FindCirclePipeParams(
                int allowableThreshold,
                int minRadius,
                int minDist,
                int maxRadius,
                int maxCannyThresh,
                int accuracy) {
            this.allowableThreshold = allowableThreshold;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.minDist = minDist;
            this.maxCannyThresh = maxCannyThresh;
            this.accuracy = accuracy;
        }
    }
}

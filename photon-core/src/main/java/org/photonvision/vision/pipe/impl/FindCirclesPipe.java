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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;

public class FindCirclesPipe
        extends CVPipe<Pair<Mat, List<Contour>>, List<CVShape>, FindCirclesPipe.FindCirclePipeParams> {
    // Output vector of found circles. Each vector is encoded as 3 or 4 element floating-point vector
    // (x,y,radius) or (x,y,radius,votes) .
    private final Mat circles = new Mat();
    /**
     * Runs the process for the pipe. The reason we need a separate pipe for circles is because if we
     * were to use the FindShapes pipe, we would have to assume that any shape more than 10-20+ sides
     * is a circle. Only issue with such approximation is that the user would no longer be able to
     * track shapes with 10-20+ sides. And hence, in order to overcome this edge case, we can use
     * HoughCircles which is more flexible and accurate for finding circles.
     *
     * @param in Input for pipe processing. 8-bit, single-channel, grayscale input image.
     * @return Result of processing.
     */
    @Override
    protected List<CVShape> process(Pair<Mat, List<Contour>> in) {
        circles.release();
        List<CVShape> output = new ArrayList<>();

        var diag = params.diagonalLengthPx;
        var minRadius = (int) (params.minRadius * diag / 100.0);
        var maxRadius = (int) (params.maxRadius * diag / 100.0);

        Imgproc.HoughCircles(
                in.getLeft(),
                circles,
                // Detection method, see #HoughModes. The available methods are #HOUGH_GRADIENT and
                // #HOUGH_GRADIENT_ALT.
                Imgproc.HOUGH_GRADIENT,
                /*Inverse ratio of the accumulator resolution to the image resolution.
                For example, if dp=1 , the accumulator has the same resolution as the input image.
                If dp=2 , the accumulator has half as big width and height. For #HOUGH_GRADIENT_ALT the recommended value is dp=1.5,
                unless some small very circles need to be detected.
                */
                1.0,
                params.minDist,
                params.maxCannyThresh,
                Math.max(1.0, params.accuracy),
                minRadius,
                maxRadius);
        // Great, we now found the center point of the circle and it's radius, but we have no idea what
        // contour it corresponds to
        // Each contour can only match to one circle, so we keep a list of unmatched contours around and
        // only match against them
        // This does mean that contours closer than allowableThreshold aren't matched to anything if
        // there's a 'better' option
        var unmatchedContours = in.getRight();
        for (int x = 0; x < circles.cols(); x++) {
            // Grab the current circle we are looking at
            double[] c = circles.get(0, x);
            // Find the center points of that circle
            double x_center = c[0];
            double y_center = c[1];

            for (Contour contour : unmatchedContours) {
                // Grab the moments of the current contour
                Moments mu = contour.getMoments();
                // Determine if the contour is within the threshold of the detected circle
                // NOTE: This means that the centroid of the contour must be within the "allowable
                // threshold"
                // of the center of the circle
                if (Math.abs(x_center - (mu.m10 / mu.m00)) <= params.allowableThreshold
                        && Math.abs(y_center - (mu.m01 / mu.m00)) <= params.allowableThreshold) {
                    // If it is, then add it to the output array
                    output.add(new CVShape(contour, new Point(c[0], c[1]), c[2]));
                    unmatchedContours.remove(contour);
                    break;
                }
            }
        }

        // Release everything we don't use
        for (var c : unmatchedContours) c.release();

        return output;
    }

    public static class FindCirclePipeParams {
        private final int allowableThreshold;
        private final int minRadius;
        private final int maxRadius;
        private final int minDist;
        private final int maxCannyThresh;
        private final int accuracy;
        private final double diagonalLengthPx;

        /*
         * @params minDist - Minimum distance between the centers of the detected circles.
         * If the parameter is too small, multiple neighbor circles may be falsely detected in addition to a true one. If it is too large, some circles may be missed.
         *
         * @param maxCannyThresh -First method-specific parameter. In case of #HOUGH_GRADIENT and #HOUGH_GRADIENT_ALT, it is the higher threshold of the two passed to the Canny edge detector (the lower one is twice smaller).
         * Note that #HOUGH_GRADIENT_ALT uses #Scharr algorithm to compute image derivatives, so the threshold value shough normally be higher, such as 300 or normally exposed and contrasty images.
         *
         *
         * @param allowableThreshold - When finding the corresponding contour, this is used to see how close a center should be to a contour for it to be considered THAT contour.
         * Should be increased with lower resolutions and decreased with higher resolution
         *  */
        public FindCirclePipeParams(
                int allowableThreshold,
                int minRadius,
                int minDist,
                int maxRadius,
                int maxCannyThresh,
                int accuracy,
                double diagonalLengthPx) {
            this.allowableThreshold = allowableThreshold;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.minDist = minDist;
            this.maxCannyThresh = maxCannyThresh;
            this.accuracy = accuracy;
            this.diagonalLengthPx = diagonalLengthPx;
        }
    }
}

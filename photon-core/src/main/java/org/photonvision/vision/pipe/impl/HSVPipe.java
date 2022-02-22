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
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.pipe.CVPipe;

public class HSVPipe extends CVPipe<Mat, Mat, HSVPipe.HSVParams> {
    @Override
    protected Mat process(Mat in) {
        var outputMat = new Mat();
        // We can save a copy here by sending the output of cvtcolor to outputMat directly
        // rather than copying. Free performance!
        Imgproc.cvtColor(in, outputMat, Imgproc.COLOR_BGR2HSV, 3);

        if (params.getHueInverted()) {
            // In Java code we do this by taking an image thresholded
            // from [0, minHue] and ORing it with [maxHue, 180]

            // we want hue from the end of the slider to max hue
            Scalar firstLower = params.getHsvLower().clone();
            Scalar firstUpper = params.getHsvUpper().clone();
            firstLower.val[0] = params.getHsvUpper().val[0];
            ;
            firstUpper.val[0] = 180;

            var lowerThresholdMat = new Mat();
            Core.inRange(outputMat, firstLower, firstUpper, lowerThresholdMat);

            // We want hue from 0 to the start of the slider
            var secondLower = params.getHsvLower().clone();
            var secondUpper = params.getHsvUpper().clone();
            secondLower.val[0] = 0;
            secondUpper.val[0] = params.getHsvLower().val[0];

            // Now that the output mat's been used by the first inRange, it's fine to mutate it
            Core.inRange(outputMat, secondLower, secondUpper, outputMat);

            // Now OR the two images together to make a mat that combines the lower and upper bounds
            // outputMat holds the second half of the range
            Core.bitwise_or(lowerThresholdMat, outputMat, outputMat);

            lowerThresholdMat.release();
        } else {
            Core.inRange(outputMat, params.getHsvLower(), params.getHsvUpper(), outputMat);
        }

        return outputMat;
    }

    public static class HSVParams {
        private final Scalar m_hsvLower;
        private final Scalar m_hsvUpper;
        private final boolean m_hueInverted;

        public HSVParams(
                IntegerCouple hue, IntegerCouple saturation, IntegerCouple value, boolean hueInverted) {
            m_hsvLower = new Scalar(hue.getFirst(), saturation.getFirst(), value.getFirst());
            m_hsvUpper = new Scalar(hue.getSecond(), saturation.getSecond(), value.getSecond());

            this.m_hueInverted = hueInverted;
        }

        public Scalar getHsvLower() {
            return m_hsvLower;
        }

        public Scalar getHsvUpper() {
            return m_hsvUpper;
        }

        public boolean getHueInverted() {
            return m_hueInverted;
        }
    }
}

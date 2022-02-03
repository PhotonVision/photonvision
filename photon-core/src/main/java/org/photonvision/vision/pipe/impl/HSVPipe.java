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
        var hsvMatrix = new Mat();
        // Converting the in matrix from BGR to HSV with 3 channel (no alpha) and saving the result in
        // hsvMatrix
        Imgproc.cvtColor(in, hsvMatrix, Imgproc.COLOR_BGR2HSV, 3);
        if (params.hueShouldInvert) {
            var tempMat = new Mat();
            // Check if hsvMatrix contains any elements between lower and upper HSV and save in our temp
            // matrix
            Core.inRange(hsvMatrix, params.getHsvLowerStart(), params.getHsvLowerEnd(), tempMat);
            // Check if hsvMatrix contains any elements between lower and upper HSV and save it in
            // hsvMatrix
            Core.inRange(hsvMatrix, params.getHsvUpperStart(), params.getHsvUpperEnd(), hsvMatrix);
            // Then take the two matrices, perform an OR on each element.
            // OR meaning every element is compared in the two matrices, and if either are 1 (meaning in
            // range of our HSV filtering), return 1
            // Store the result in hsvMatrix
            Core.bitwise_or(tempMat, hsvMatrix, hsvMatrix);
        } else {
            Core.inRange(outputMat, params.getHsvLower(), params.getHsvUpper(), outputMat);
        }
        return outputMat;
    }

    public static class HSVParams {
        private final Scalar m_hsvLower;
        private final Scalar m_hsvUpper;
        private final boolean hueShouldInvert;

        private final Scalar m_hsvLowerStart;
        private final Scalar m_hsvLowerEnd;
        private final Scalar m_hsvUpperStart;
        private final Scalar m_hsvUpperEnd;

        public HSVParams(
                IntegerCouple hue, IntegerCouple saturation, IntegerCouple value, boolean hueShouldInvert) {
            m_hsvLower = new Scalar(hue.getFirst(), saturation.getFirst(), value.getFirst());
            m_hsvUpper = new Scalar(hue.getSecond(), saturation.getSecond(), value.getSecond());
            hueShouldInvert = hueShouldInvert;

            if (hueShouldInvert) {
                // Hue is limited to numbers between 0->255. We have to map our ranges within these bounds
                // Since hue is circular, we might want to have a range like X -> 255 & 0 -> Y
                // For this, we must specify two ranges
                // One range from 0 -> Lower Bound, another from Upper Bound -> 255

                // 0->X
                m_hsvLowerStart = new Scaler(0, saturation.getFirst(), value.getFirst());
                m_hsvLowerEnd = new Scalar(hue.getFirst(), saturation.getSecond(), value.getSecond());

                // Y->255
                m_hsvUpperStart = new Scaler(hue.getSecond(), saturation.getFirst(), value.getFirst());
                m_hsvUpperEnd = new Scalar(255, saturation.getSecond(), value.getSecond());
            }
        }

        public Scalar getHsvLower() {
            return m_hsvLower;
        }

        public Scalar getHsvUpper() {
            return m_hsvUpper;
        }

        public boolean getHueShouldInvert() {
            return hueShouldInvert;
        }

        public Scalar getHsvLowerStart() {
            return m_hsvLowerStart;
        }

        public Scalar getHsvLowerEnd() {
            return m_hsvLowerEnd;
        }

        public Scalar getHsvUpperStart() {
            return m_hsvUpperStart;
        }

        public Scalar getHsvUpperEnd() {
            return m_hsvUpperEnd;
        }
    }
}

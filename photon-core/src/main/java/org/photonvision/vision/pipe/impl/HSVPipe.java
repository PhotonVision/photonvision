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
        in.copyTo(outputMat);
        Imgproc.cvtColor(outputMat, outputMat, Imgproc.COLOR_BGR2HSV, 3);
        Core.inRange(outputMat, params.getHsvLower(), params.getHsvUpper(), outputMat);
        return outputMat;
    }

    public static class HSVParams {
        private final Scalar m_hsvLower;
        private final Scalar m_hsvUpper;

        public HSVParams(IntegerCouple hue, IntegerCouple saturation, IntegerCouple value) {
            m_hsvLower = new Scalar(hue.getFirst(), saturation.getFirst(), value.getFirst());
            m_hsvUpper = new Scalar(hue.getSecond(), saturation.getSecond(), value.getSecond());
        }

        public Scalar getHsvLower() {
            return m_hsvLower;
        }

        public Scalar getHsvUpper() {
            return m_hsvUpper;
        }
    }
}

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

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.opencv.DualMat;
import org.photonvision.vision.pipe.CVPipe;

public class OutputMatPipe extends CVPipe<DualMat, Mat, OutputMatPipe.OutputMatParams> {

    private Mat m_outputMat = new Mat();

    @Override
    protected Mat process(DualMat in) {
        Mat rawCam = in.first;
        Mat hsv = in.second;
        if (params.showThreshold()) {
            // convert input mat
            try {
                hsv.copyTo(m_outputMat);
                Imgproc.cvtColor(m_outputMat, m_outputMat, Imgproc.COLOR_GRAY2BGR, 3);
            } catch (CvException e) {
                System.err.println("(OutputMatPipe) Exception thrown by OpenCV: \n" + e.getMessage());
            }
        } else {
            m_outputMat = rawCam;
        }

        return m_outputMat;
    }

    public static class OutputMatParams {
        private boolean m_showThreshold;

        public OutputMatParams(boolean showThreshold) {
            m_showThreshold = showThreshold;
        }

        public boolean showThreshold() {
            return m_showThreshold;
        }
    }
}

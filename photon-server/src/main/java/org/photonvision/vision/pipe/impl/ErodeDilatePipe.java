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

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

public class ErodeDilatePipe extends CVPipe<Mat, Mat, ErodeDilatePipe.ErodeDilateParams> {
    @Override
    protected Mat process(Mat in) {
        if (params.shouldErode()) {
            Imgproc.erode(in, in, params.getKernel());
        }
        if (params.shouldDilate()) {
            Imgproc.dilate(in, in, params.getKernel());
        }
        return in;
    }

    public static class ErodeDilateParams {
        private boolean m_erode;
        private boolean m_dilate;
        private Mat m_kernel;

        public ErodeDilateParams(boolean erode, boolean dilate, int kernelSize) {
            m_erode = erode;
            m_dilate = dilate;
            m_kernel =
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        }

        public boolean shouldErode() {
            return m_erode;
        }

        public boolean shouldDilate() {
            return m_dilate;
        }

        public Mat getKernel() {
            return m_kernel;
        }
    }
}

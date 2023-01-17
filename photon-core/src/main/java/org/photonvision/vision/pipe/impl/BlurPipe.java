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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.MutatingPipe;

/** Represents a pipeline that blurs the image. */
public class BlurPipe extends MutatingPipe<Mat, BlurPipe.BlurParams> {
    /**
     * Processes this pipe.
     *
     * @param in Input for pipe processing.
     * @return The processed frame.
     */
    @Override
    protected Void process(Mat in) {
        Imgproc.blur(in, in, params.getBlurSize());
        return null;
    }

    public static class BlurParams {
        // Default BlurImagePrams with zero blur.
        public static BlurParams DEFAULT = new BlurParams(0);

        // Member to store the blur size.
        private final int m_blurSize;

        /**
         * Constructs a new BlurImageParams.
         *
         * @param blurSize The blur size.
         */
        public BlurParams(int blurSize) {
            m_blurSize = blurSize;
        }

        /**
         * Returns the blur size.
         *
         * @return The blur size.
         */
        public Size getBlurSize() {
            return new Size(m_blurSize, m_blurSize);
        }
    }
}

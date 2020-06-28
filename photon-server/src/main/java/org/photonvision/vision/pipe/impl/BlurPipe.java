package org.photonvision.vision.pipe.impl;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

/** Represents a pipeline that blurs the image. */
public class BlurPipe extends CVPipe<Mat, Mat, BlurPipe.BlurParams> {
    /**
    * Processes thos pipe.
    *
    * @param in Input for pipe processing.
    * @return The processed frame.
    */
    @Override
    protected Mat process(Mat in) {
        Imgproc.blur(in, in, params.getBlurSize());
        return in;
    }

    public static class BlurParams {
        // Default BlurImagePrams with zero blur.
        public static BlurParams DEFAULT = new BlurParams(0);

        // Member to store the blur size.
        private int m_blurSize;

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

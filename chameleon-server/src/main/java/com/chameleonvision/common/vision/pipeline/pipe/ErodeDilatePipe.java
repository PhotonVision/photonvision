package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.vision.pipeline.CVPipe;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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

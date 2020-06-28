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

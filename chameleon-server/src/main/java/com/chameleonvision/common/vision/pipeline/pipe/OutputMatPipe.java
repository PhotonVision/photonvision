package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.vision.pipeline.CVPipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class OutputMatPipe extends CVPipe<Pair<Mat, Mat>, Mat, OutputMatPipe.OutputMatParams> {

    private Mat m_outputMat = new Mat();

    @Override
    protected Mat process(Pair<Mat, Mat> in) {
        if (params.showThreshold()) {
            try {
                in.getRight().copyTo(m_outputMat);
                Imgproc.cvtColor(m_outputMat, m_outputMat, Imgproc.COLOR_GRAY2BGR, 3);
            } catch (CvException e) {
                System.err.println("(OutputMatPipe) Exception thrown by OpenCV: \n" + e.getMessage());
            }
        } else {
            in.getLeft().copyTo(m_outputMat);
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

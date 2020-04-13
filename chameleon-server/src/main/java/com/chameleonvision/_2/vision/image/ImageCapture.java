package com.chameleonvision._2.vision.image;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public interface ImageCapture {
    /**
    * Get the next camera frame
    *
    * @return a Pair of the captured image and the Linux epoch of when the frame was grabbed (in uS)
    */
    Pair<Mat, Long> getFrame();
}

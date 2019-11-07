package com.chameleonvision.classabstraction.camera;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public interface CameraProcess {
    Pair<Mat, Long> getFrame(Mat frame);

    void setExposure(int exposure);
    void setBrightness(int brightness);
}

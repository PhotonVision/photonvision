package com.chameleonvision.classabstraction.camera;

import org.opencv.core.Mat;

public interface CameraStreamer {
    void streamFrame(Mat frame);
}

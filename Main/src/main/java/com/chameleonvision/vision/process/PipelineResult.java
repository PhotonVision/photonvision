package com.chameleonvision.vision.process;

import org.opencv.core.RotatedRect;

public class PipelineResult {
    public boolean IsValid = false;
    public double CalibratedX = 0.0;
    public double CalibratedY = 0.0;
    public double Pitch = 0.0;
    public double Yaw = 0.0;
    public double Area = 0.0;
    RotatedRect RawPoint;
}

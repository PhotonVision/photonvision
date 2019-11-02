package com.chameleonvision.vision.process;

import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.CameraValues;
import org.opencv.core.Mat;

public interface CVProcess {
    PipelineResult runPipeline(Pipeline currentPipeline, Mat inputImage, Mat outputImage, CameraValues cameraValues, boolean shouldFlip, boolean driverMode);
}

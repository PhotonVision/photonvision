package com.chameleonvision.classabstraction.pipeline;

import org.opencv.core.Mat;

public class DriverVisionPipeline extends CVPipeline<Void> {
    public DriverVisionPipeline(CVPipelineSettings settings) {
        super(settings);
    }

    @Override
    void initPipeline() {
        // TODO set exposure/brightness of camera
    }

    @Override
    Void runPipeline(Mat inputMat) {
        this.outputMat = inputMat;
        return null;
    }

    @Override
    Mat getOutputMat() {
        return this.outputMat;
    }
}

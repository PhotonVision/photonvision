package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.USBCamera;
import com.chameleonvision.vision.process.PipelineResult;
import org.opencv.core.Mat;

public class DriverVisionPipeline extends CVPipeline<DriverVisionPipeline.DriverPipelineResult, CVPipelineSettings> {
    public DriverVisionPipeline(CVPipelineSettings settings) {
        super(settings);
    }

    @Override
    void initPipeline(USBCamera camera) {
        // TODO: set camera to driver mode
    }

    @Override
    DriverPipelineResult runPipeline(Mat inputMat) {
        return new DriverPipelineResult(inputMat);
    }

    public static class DriverPipelineResult extends CVPipelineResult<Void> {
        public DriverPipelineResult(Mat outputMat) {
            this.hasTarget = false;
            this.targets = null;
            outputMat.copyTo(this.outputMat);
        }
    }
}

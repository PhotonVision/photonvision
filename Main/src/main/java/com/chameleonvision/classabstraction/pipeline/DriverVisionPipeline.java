package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import org.opencv.core.Mat;

import java.util.List;
import java.util.function.Supplier;

import static com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline.*;

public class DriverVisionPipeline extends CVPipeline<DriverPipelineResult, CVPipelineSettings> {

    public DriverVisionPipeline(Supplier<CVPipelineSettings> settingsSupplier) {
        super(settingsSupplier);
    }

    @Override
    public void initPipeline(CameraProcess camera) {
        camera.setBrightness((int) getSettings().brightness);
        camera.setExposure((int) getSettings().exposure);
    }

    @Override
    public DriverPipelineResult runPipeline(Mat inputMat) {

        inputMat.copyTo(outputMat);

        return new DriverPipelineResult(null, inputMat);
    }

    public static class DriverPipelineResult extends CVPipelineResult<Void> {
        public DriverPipelineResult(List<Void> targets, Mat outputMat) {
            super(targets, outputMat);
        }
    }
}

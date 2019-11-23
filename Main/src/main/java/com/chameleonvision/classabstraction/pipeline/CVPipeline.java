package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import org.opencv.core.Mat;

import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 *
 * @param <R> Pipeline result type
 */
public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected Mat outputMat = new Mat();
    CameraProcess cameraProcess;
    public final S settings;

    protected CVPipeline(S settings) {
        this.settings = settings;
    }

    protected CVPipeline(String pipelineName, S settings) {
        this.settings = settings;
        settings.nickname = pipelineName;
    }

    public void initPipeline(CameraProcess camera) {
        cameraProcess = camera;
        cameraProcess.setExposure((int) settings.exposure);
        cameraProcess.setBrightness((int) settings.brightness);
    }
    abstract public R runPipeline(Mat inputMat);
}

package com.chameleonvision.vision.pipeline;

import com.chameleonvision.vision.camera.CameraCapture;
import org.opencv.core.Mat;

/**
 *
 * @param <R> Pipeline result type
 */
public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected Mat outputMat = new Mat();
    CameraCapture cameraCapture;
    public S settings;

    protected CVPipeline(S settings) {
        this.settings = settings;
    }

    protected CVPipeline(String pipelineName, S settings) {
        this.settings = settings;
        settings.nickname = pipelineName;
    }

    public void initPipeline(CameraCapture camera) {
        cameraCapture = camera;
        cameraCapture.setExposure((int) settings.exposure);
        cameraCapture.setBrightness((int) settings.brightness);
        cameraCapture.setGain((int) settings.gain);
    }
    abstract public R runPipeline(Mat inputMat);

    public boolean is3D() {
        return (this instanceof CVPipeline3d);
    }
}

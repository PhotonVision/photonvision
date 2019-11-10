package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import org.opencv.core.Mat;

import java.util.function.Supplier;

/**
 *
 * @param <R> Pipeline result type
 */
public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected Mat outputMat;
    CameraProcess cameraProcess;
    final Supplier<S> settingsSupplier;

    public CVPipeline(Supplier<S> settingsSupplier) {
        this.settingsSupplier = settingsSupplier;
    }

    public S getSettings() {
      return settingsSupplier.get();
    }

    public void initPipeline(CameraProcess camera) {
        cameraProcess = camera;
        cameraProcess.setExposure((int) getSettings().exposure);
        cameraProcess.setBrightness((int) getSettings().brightness);
    }
    abstract public R runPipeline(Mat inputMat);
}

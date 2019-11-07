package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import org.opencv.core.Mat;

/**
 *
 * @param <R> Pipeline result type
 */
public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected S settings;
    private Mat inputMat;
    protected Mat outputMat;

    public CVPipeline(S settings) {
        this.settings = settings;
    }

    abstract void initPipeline(CameraProcess camera);
    abstract R runPipeline(Mat inputMat);
}

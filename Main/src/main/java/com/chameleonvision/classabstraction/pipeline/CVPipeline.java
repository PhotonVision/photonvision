package com.chameleonvision.classabstraction.pipeline;

import org.opencv.core.Mat;

/**
 *
 * @param <R> Pipeline result type
 */
public abstract class CVPipeline<R> {
    private CVPipelineSettings settings;
    private Mat inputMat;
    protected Mat outputMat;

    public CVPipeline(CVPipelineSettings settings) {
        this.settings = settings;
    }

    abstract void initPipeline();
    abstract R runPipeline(Mat inputMat);
    abstract Mat getOutputMat();
}

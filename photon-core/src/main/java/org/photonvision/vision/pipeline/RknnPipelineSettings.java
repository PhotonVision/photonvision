
package org.photonvision.vision.pipeline;

public class RknnPipelineSettings extends AdvancedPipelineSettings {
    public double confidence = 90.0;

    public RknnPipelineSettings() {
        super();
        this.pipelineType = PipelineType.Rknn;
        this.outputShowMultipleTargets = true;
        cameraExposure = 20;
        cameraAutoExposure = false;
        ledMode = false;
        double confidence = 90.0;
    }
}
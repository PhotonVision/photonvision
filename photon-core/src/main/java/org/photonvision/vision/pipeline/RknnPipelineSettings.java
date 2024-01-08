
package org.photonvision.vision.pipeline;

public class RknnPipelineSettings extends AdvancedPipelineSettings {
    double confidence;

    public RknnPipelineSettings() {
        super();
        this.pipelineType = PipelineType.Rknn;
        
        // Sane defaults
        this.outputShowMultipleTargets = true;
        cameraExposure = 20;
        cameraAutoExposure = false;
        ledMode = false;
    }
}
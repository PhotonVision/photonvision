
package org.photonvision.vision.pipeline;

public class RknnPipelineSettings extends AdvancedPipelineSettings {
    public double confidence;
    public double nms; //non maximal suppression
    public double max_detections;

    public RknnPipelineSettings() {
        super();
        this.pipelineType = PipelineType.Rknn;
        this.outputShowMultipleTargets = true;
        cameraExposure = 20;
        cameraAutoExposure = false;
        ledMode = false;
        confidence = 90.0;
        nms = .45;
        max_detections = 10;
    }
}
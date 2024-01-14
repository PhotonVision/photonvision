package org.photonvision.vision.pipeline;

public class ObjectDetectionPipelineSettings extends AdvancedPipelineSettings {
    public double confidence;
    public double nms; // non maximal suppression
    public double box_thresh;

    public ObjectDetectionPipelineSettings() {
        super();
        this.pipelineType = PipelineType.ObjectDetection; // TODO: FIX this
        this.outputShowMultipleTargets = true;
        cameraExposure = 20;
        cameraAutoExposure = false;
        ledMode = false;
        confidence = .9;
        nms = .45;
        box_thresh = .25;
    }
}

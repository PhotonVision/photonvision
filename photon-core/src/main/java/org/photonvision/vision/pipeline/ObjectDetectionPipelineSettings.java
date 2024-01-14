
package org.photonvision.vision.pipeline;

public class ObjectDetectionPipelineSettings extends AdvancedPipelineSettings {
    public double confidence;
    public double nms; //non maximal suppression
    public double max_detections;

    public ObjectDetectionPipelineSettings() {
        super();
        this.pipelineType = PipelineType.ObjectDetection; //TODO: FIX this 
        this.outputShowMultipleTargets = true;
        cameraExposure = 20;
        cameraAutoExposure = false;
        ledMode = false;
        confidence = 90.0;
        nms = .45;
        max_detections = 10;
    }
}
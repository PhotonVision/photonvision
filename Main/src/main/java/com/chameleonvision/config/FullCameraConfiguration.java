package com.chameleonvision.config;

import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.util.List;

public class FullCameraConfiguration {
    public final CameraJsonConfig cameraConfig;
    public final List<CVPipelineSettings> pipelines;
    public final CVPipelineSettings drivermode;


    public FullCameraConfiguration(CameraJsonConfig cameraConfig, List<CVPipelineSettings> pipelines, CVPipelineSettings drivermode) {
        this.cameraConfig = cameraConfig;
        this.pipelines = pipelines;
        this.drivermode = drivermode;
    }
}

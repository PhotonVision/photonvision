package com.chameleonvision.config;

import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.util.List;

public class FullCameraConfiguration {
    public final CameraJsonConfig cameraConfig;
    public final List<CVPipelineSettings> pipelines;
    public final CVPipelineSettings drivermode;
    public final CameraConfig fileConfig;

    FullCameraConfiguration(CameraJsonConfig cameraConfig, List<CVPipelineSettings> pipelines, CVPipelineSettings drivermode, CameraConfig fileConfig) {
        this.cameraConfig = cameraConfig;
        this.pipelines = pipelines;
        this.drivermode = drivermode;
        this.fileConfig = fileConfig;
    }
}

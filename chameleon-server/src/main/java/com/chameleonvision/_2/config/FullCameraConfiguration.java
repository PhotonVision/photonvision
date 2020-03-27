package com.chameleonvision._2.config;

import com.chameleonvision._2.vision.pipeline.CVPipelineSettings;

import java.util.List;

public class FullCameraConfiguration {
    public final CameraJsonConfig cameraConfig;
    public final List<CVPipelineSettings> pipelines;
    public final CVPipelineSettings driverMode;
    public final List<CameraCalibrationConfig> calibration;
    public final CameraConfig fileConfig;

    FullCameraConfiguration(CameraJsonConfig cameraConfig, List<CVPipelineSettings> pipelines, CVPipelineSettings driverMode, List<CameraCalibrationConfig> calibration, CameraConfig fileConfig) {
        this.cameraConfig = cameraConfig;
        this.pipelines = pipelines;
        this.driverMode = driverMode;
        this.calibration = calibration;
        this.fileConfig = fileConfig;
    }
}

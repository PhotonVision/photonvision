package com.chameleonvision.config;

import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.CVPipeline2dSettings;
import com.chameleonvision.vision.pipeline.CVPipeline3dSettings;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.io.IOException;
import java.util.List;

public class PipelineConfig {

    public static final String CVPipeline2DPrefix = "CV2D";
    public static final String CVPipeline3DPrefix = "CV3D";

    private final CameraConfig cameraConfig;

    public PipelineConfig(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    void check() {
        if (!cameraConfig.pipelinesExists()) {
            save(new CVPipeline2dSettings());
        }
    }

    private void save(CVPipelineSettings settings) {
        if (settings instanceof CVPipeline2dSettings) {

        } else if (settings instanceof CVPipeline3dSettings) {

        }
    }

    public void save(List<CVPipelineSettings> settings) {
        for(CVPipelineSettings setting : settings) {
            save(setting);
        }
    }

    public List<CVPipelineSettings> load() {
        return null;
    }
}

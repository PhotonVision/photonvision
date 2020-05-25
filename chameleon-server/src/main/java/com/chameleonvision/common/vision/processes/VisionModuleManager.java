package com.chameleonvision.common.vision.processes;

import java.util.ArrayList;
import java.util.List;

/** VisionModuleManager has many VisionModules, and provides camera configuration data to them. */
public class VisionModuleManager {
    protected final List<VisionModule> visionModules = new ArrayList<>();

    public VisionModuleManager(List<VisionSource> visionSources) {
        for (var visionSource : visionSources) {

            // TODO: loading existing pipelines from config
            var pipelineManager = new PipelineManager();

            visionModules.add(new VisionModule(pipelineManager, visionSource));
        }
    }

    public void startModules() {
        for (var visionModule : visionModules) {
            visionModule.start();
        }
    }
}

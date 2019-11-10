package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import com.chameleonvision.classabstraction.pipeline.CVPipeline;
import com.chameleonvision.classabstraction.pipeline.CVPipelineSettings;
import com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline;

import java.util.ArrayList;
import java.util.List;

public class VisionProcess {

    private final CameraProcess cameraProcess;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private CVPipeline currentPipeline;

    private final CVPipelineSettings driverVisionSettings = new CVPipelineSettings();

    public VisionProcess(CameraProcess cameraProcess) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(pipelines.get(0));
    }

    public void setPipeline(int pipelineIndex) {
        CVPipeline newPipeline = pipelines.get(pipelineIndex);
        if (newPipeline != null) {
            setPipeline(newPipeline);
        }
    }

    public void setPipeline(CVPipeline pipeline) {
        currentPipeline = pipeline;
        currentPipeline.initPipeline(cameraProcess);
    }

    public CVPipeline getCurrentPipeline() {
        return currentPipeline;
    }
}

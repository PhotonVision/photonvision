package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;

public class Calibration3dPipeline extends CVPipeline<CVPipelineResult, CVPipelineSettings> {

    // TODO: Everything here

    public Calibration3dPipeline() {
        settings = new CVPipelineSettings();
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, CVPipelineSettings settings) {}

    @Override
    protected CVPipelineResult process(Frame frame, CVPipelineSettings settings) {
        return null;
    }
}

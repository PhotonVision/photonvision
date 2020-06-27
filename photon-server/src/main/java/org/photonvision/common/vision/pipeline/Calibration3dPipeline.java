package org.photonvision.common.vision.pipeline;

import org.photonvision.common.vision.frame.Frame;
import org.photonvision.common.vision.frame.FrameStaticProperties;

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

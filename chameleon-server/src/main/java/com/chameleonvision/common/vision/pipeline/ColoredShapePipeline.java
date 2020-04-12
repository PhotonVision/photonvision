package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;

public class ColoredShapePipeline
        extends CVPipeline<CVPipelineResult, ColoredShapePipelineSettings> {
    @Override
    protected void setPipeParams(
            ColoredShapePipelineSettings settings, FrameStaticProperties frameStaticProperties) {}

    @Override
    protected CVPipelineResult process(Frame frame, ColoredShapePipelineSettings settings) {
        return null;
    }
}

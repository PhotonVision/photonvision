package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.Frame;
import java.util.List;

public class DriverModePipelineResult extends CVPipelineResult {
    public DriverModePipelineResult(double latencyMillis, Frame outputFrame) {
        super(latencyMillis, List.of(), outputFrame);
    }
}

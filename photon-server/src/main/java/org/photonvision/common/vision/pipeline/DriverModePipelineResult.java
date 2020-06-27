package org.photonvision.common.vision.pipeline;

import org.photonvision.common.vision.frame.Frame;

import java.util.List;

public class DriverModePipelineResult extends CVPipelineResult {
    public DriverModePipelineResult(double latencyMillis, Frame outputFrame) {
        super(latencyMillis, List.of(), outputFrame);
    }
}

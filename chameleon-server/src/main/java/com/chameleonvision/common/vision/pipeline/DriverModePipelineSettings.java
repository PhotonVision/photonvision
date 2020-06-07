package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.target.RobotOffsetPointMode;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DriverModePipelineSettings")
public class DriverModePipelineSettings extends CVPipelineSettings {
    public RobotOffsetPointMode offsetPointMode = RobotOffsetPointMode.None;
    public DoubleCouple offsetPoint = new DoubleCouple();

    public DriverModePipelineSettings() {
        super();
        pipelineType = PipelineType.DriverMode;
    }
}

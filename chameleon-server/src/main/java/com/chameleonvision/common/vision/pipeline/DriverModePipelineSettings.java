package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.target.RobotOffsetPointMode;

public class DriverModePipelineSettings extends CVPipelineSettings {
    public RobotOffsetPointMode offsetPointMode = RobotOffsetPointMode.None;
    public DoubleCouple offsetPoint = new DoubleCouple();
}

package org.photonvision.common.vision.pipeline;

import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.vision.target.RobotOffsetPointMode;
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

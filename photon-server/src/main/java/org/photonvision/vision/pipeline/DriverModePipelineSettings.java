package org.photonvision.vision.pipeline;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.target.RobotOffsetPointMode;

@JsonTypeName("DriverModePipelineSettings")
public class DriverModePipelineSettings extends CVPipelineSettings {
    public RobotOffsetPointMode offsetPointMode = RobotOffsetPointMode.None;
    public DoubleCouple offsetPoint = new DoubleCouple();

    public DriverModePipelineSettings() {
        super();
        pipelineType = PipelineType.DriverMode;
    }
}

package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.opencv.ContourShape;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;

@JsonTypeName("ColoredShapePipelineSettings")
public class ColoredShapePipelineSettings extends AdvancedPipelineSettings {
    ContourShape desiredShape;

    public ColoredShapePipelineSettings() {
        super();
        pipelineType = PipelineType.ColoredShape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColoredShapePipelineSettings that = (ColoredShapePipelineSettings) o;
        return desiredShape == that.desiredShape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), desiredShape);
    }
}

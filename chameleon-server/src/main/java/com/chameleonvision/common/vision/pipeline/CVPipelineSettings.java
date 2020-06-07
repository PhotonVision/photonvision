package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.FrameDivisor;
import com.chameleonvision.common.vision.pipe.ImageFlipMode;
import com.chameleonvision.common.vision.pipe.ImageRotationMode;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY,
        property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ColoredShapePipelineSettings.class),
    @JsonSubTypes.Type(value = ReflectivePipelineSettings.class),
    @JsonSubTypes.Type(value = DriverModePipelineSettings.class)
})
public class CVPipelineSettings {
    public int pipelineIndex = 0;
    public PipelineType pipelineType = PipelineType.DriverMode;
    public ImageFlipMode inputImageFlipMode = ImageFlipMode.NONE;
    public ImageRotationMode inputImageRotationMode = ImageRotationMode.DEG_0;
    public String pipelineNickname = "New Pipeline";
    public double cameraExposure = 50.0;
    public double cameraBrightness = 50.0;
    public double cameraGain = 50.0;
    public int cameraVideoModeIndex = 0;
    public FrameDivisor inputFrameDivisor = FrameDivisor.NONE;
    public FrameDivisor outputFrameDivisor = FrameDivisor.NONE;
    public boolean ledMode = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CVPipelineSettings that = (CVPipelineSettings) o;
        return pipelineIndex == that.pipelineIndex
                && Double.compare(that.cameraExposure, cameraExposure) == 0
                && Double.compare(that.cameraBrightness, cameraBrightness) == 0
                && Double.compare(that.cameraGain, cameraGain) == 0
                && cameraVideoModeIndex == that.cameraVideoModeIndex
                && ledMode == that.ledMode
                && pipelineType == that.pipelineType
                && inputImageFlipMode == that.inputImageFlipMode
                && inputImageRotationMode == that.inputImageRotationMode
                && pipelineNickname.equals(that.pipelineNickname)
                && inputFrameDivisor == that.inputFrameDivisor
                && outputFrameDivisor == that.outputFrameDivisor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pipelineIndex,
                pipelineType,
                inputImageFlipMode,
                inputImageRotationMode,
                pipelineNickname,
                cameraExposure,
                cameraBrightness,
                cameraGain,
                cameraVideoModeIndex,
                inputFrameDivisor,
                outputFrameDivisor,
                ledMode);
    }
}

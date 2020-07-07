/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.pipeline;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.ImageFlipMode;
import org.photonvision.vision.pipe.ImageRotationMode;

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
    public int cameraExposure = 50;
    public int cameraBrightness = 50;
    public int cameraGain = 50;
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

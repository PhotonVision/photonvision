/*
 * Copyright (C) Photon Vision.
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
import org.photonvision.vision.opencv.ImageRotationMode;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY,
        property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ColoredShapePipelineSettings.class),
    @JsonSubTypes.Type(value = ReflectivePipelineSettings.class),
    @JsonSubTypes.Type(value = DriverModePipelineSettings.class),
    @JsonSubTypes.Type(value = AprilTagPipelineSettings.class),
    @JsonSubTypes.Type(value = ArucoPipelineSettings.class)
})
public class CVPipelineSettings implements Cloneable {
    public int pipelineIndex = 0;
    public PipelineType pipelineType = PipelineType.DriverMode;
    public ImageRotationMode inputImageRotationMode = ImageRotationMode.DEG_0;
    public String pipelineNickname = "New Pipeline";
    public boolean cameraAutoExposure = false;
    // manual exposure only used if cameraAutoExposure if false
    public double cameraExposure = 20;
    public int cameraBrightness = 50;
    // Currently only used by a few cameras (notably the zero-copy Pi Camera driver) with the Gain
    // quirk
    public int cameraGain = 75;
    // Currently only used by the zero-copy Pi Camera driver
    public int cameraRedGain = 11;
    public int cameraBlueGain = 20;
    public int cameraVideoModeIndex = 0;
    public FrameDivisor streamingFrameDivisor = FrameDivisor.NONE;
    public boolean ledMode = false;
    public boolean inputShouldShow = false;
    public boolean outputShouldShow = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CVPipelineSettings that = (CVPipelineSettings) o;
        return pipelineIndex == that.pipelineIndex
                && Double.compare(that.cameraExposure, cameraExposure) == 0
                && Double.compare(that.cameraBrightness, cameraBrightness) == 0
                && Double.compare(that.cameraGain, cameraGain) == 0
                && Double.compare(that.cameraRedGain, cameraRedGain) == 0
                && Double.compare(that.cameraBlueGain, cameraBlueGain) == 0
                && cameraVideoModeIndex == that.cameraVideoModeIndex
                && ledMode == that.ledMode
                && pipelineType == that.pipelineType
                && inputImageRotationMode == that.inputImageRotationMode
                && pipelineNickname.equals(that.pipelineNickname)
                && streamingFrameDivisor == that.streamingFrameDivisor
                && inputShouldShow == that.inputShouldShow
                && outputShouldShow == that.outputShouldShow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pipelineIndex,
                pipelineType,
                inputImageRotationMode,
                pipelineNickname,
                cameraExposure,
                cameraBrightness,
                cameraGain,
                cameraRedGain,
                cameraBlueGain,
                cameraVideoModeIndex,
                streamingFrameDivisor,
                ledMode,
                inputShouldShow,
                outputShouldShow);
    }

    @Override
    public CVPipelineSettings clone() {
        try {
            return (CVPipelineSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "CVPipelineSettings{"
                + "pipelineIndex="
                + pipelineIndex
                + ", pipelineType="
                + pipelineType
                + ", inputImageRotationMode="
                + inputImageRotationMode
                + ", pipelineNickname='"
                + pipelineNickname
                + '\''
                + ", cameraExposure="
                + cameraExposure
                + ", cameraBrightness="
                + cameraBrightness
                + ", cameraGain="
                + cameraGain
                + ", cameraRedGain="
                + cameraRedGain
                + ", cameraBlueGain="
                + cameraBlueGain
                + ", cameraVideoModeIndex="
                + cameraVideoModeIndex
                + ", streamingFrameDivisor="
                + streamingFrameDivisor
                + ", ledMode="
                + ledMode
                + ", inputShouldShow="
                + inputShouldShow
                + ", outputShouldShow="
                + outputShouldShow
                + '}';
    }
}

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

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ReflectivePipelineSettings")
public class ReflectivePipelineSettings extends AdvancedPipelineSettings {
    public double contourFilterRangeX = 2;
    public double contourFilterRangeY = 2;

    public ReflectivePipelineSettings() {
        super();
        pipelineType = PipelineType.Reflective;
        cameraExposure = 6;
        cameraGain = 20;
    }
}

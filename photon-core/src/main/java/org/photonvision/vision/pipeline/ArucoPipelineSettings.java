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
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("ArucoPipelineSettings")
public class ArucoPipelineSettings extends AdvancedPipelineSettings {
    public double decimate = 1;
    public int threads = 2;
    public int numIterations = 100;
    public double cornerAccuracy = 25.0;
    public boolean useAruco3 = true;

    // 3d settings

    public ArucoPipelineSettings() {
        super();
        pipelineType = PipelineType.Aruco;
        outputShowMultipleTargets = true;
        targetModel = TargetModel.kAruco6in_16h5;
        cameraExposure = -1;
        cameraAutoExposure = true;
        ledMode = false;
    }
}

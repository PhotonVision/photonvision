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
import org.opencv.aruco.Aruco;
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("ArucoPipelineSettings")
public class ArucoPipelineSettings extends AdvancedPipelineSettings {
    public int threshMinSize = 3;
    public int threshStepSize = 10;
    public int threshMaxSize = 23;
    public int threshConstant = 7;
    public double errorCorrectionRate = 0.5;
    public boolean useCornerRefinement = true;
    public int refineNumIterations = 30;
    public double refineMinErrorPx = 0.1;
    public int refineWinSize = 5;
    public int cornerRefinementStrategy = Aruco.CORNER_REFINE_SUBPIX;
    public boolean useAruco3 = false;
    public double aruco3MinMarkerSideRatio = 0.02;
    public int aruco3MinCanonicalImgSide = 32;

    public ArucoPipelineSettings() {
        super();
        pipelineType = PipelineType.Aruco;
        outputShowMultipleTargets = true;
        targetModel = TargetModel.k6in_16h5;
        cameraExposure = -1;
        cameraAutoExposure = true;
        ledMode = false;
    }
}

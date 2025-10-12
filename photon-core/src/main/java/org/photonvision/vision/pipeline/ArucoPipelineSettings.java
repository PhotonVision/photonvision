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
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("ArucoPipelineSettings")
public class ArucoPipelineSettings extends AdvancedPipelineSettings {
    public AprilTagFamily tagFamily = AprilTagFamily.kTag36h11;

    public IntegerCouple threshWinSizes = new IntegerCouple(11, 91);
    public int threshStepSize = 40;
    public double threshConstant = 10;
    public boolean debugThreshold = false;

    public boolean useCornerRefinement = true;
    public int refineNumIterations = 30;
    public double refineMinErrorPx = 0.005;

    public boolean useAruco3 = false;
    public double aruco3MinMarkerSideRatio = 0.02;
    public int aruco3MinCanonicalImgSide = 32;

    public boolean doMultiTarget = false;
    public boolean doSingleTargetAlways = false;

    public ArucoPipelineSettings() {
        super();
        pipelineType = PipelineType.Aruco;
        outputShowMultipleTargets = true;
        targetModel = TargetModel.kAprilTag6p5in_36h11;
        cameraExposureRaw = 20;
        cameraAutoExposure = true;
        ledMode = false;
    }
}

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
import java.util.Objects;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("AprilTagPipelineSettings")
public class AprilTagPipelineSettings extends AdvancedPipelineSettings {
    public AprilTagFamily tagFamily = AprilTagFamily.kTag36h11;
    public double decimate = 1.0;
    public double blur = 0;
    public int threads = 1;
    public boolean debug = false;
    public boolean refineEdges = true;
    public int numIterations = 200;

    // TODO is this a legit, reasonable default?
    public int hammingDist = 1;
    public int decisionMargin = 30;

    // 3d settings

    public AprilTagPipelineSettings() {
        super();
        pipelineType = PipelineType.AprilTag;
        outputShowMultipleTargets = true;
        targetModel = TargetModel.k200mmAprilTag;
        cameraExposure = -1;
        cameraAutoExposure = true;
        ledMode = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AprilTagPipelineSettings that = (AprilTagPipelineSettings) o;
        return Objects.equals(tagFamily, that.tagFamily)
                && Double.compare(decimate, that.decimate) == 0
                && Double.compare(blur, that.blur) == 0
                && threads == that.threads
                && debug == that.debug
                && refineEdges == that.refineEdges;
    }
}

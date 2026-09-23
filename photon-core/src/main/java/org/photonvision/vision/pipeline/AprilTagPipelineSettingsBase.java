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

import org.photonvision.vision.apriltag.AprilTagFamily;

/**
 * Settings shared by every AprilTag detector backend - CPU ({@link AprilTagPipelineSettings}) and
 * Vulkan ({@code VkAprilTagPipelineSettings}) alike. These are consumed by {@link
 * AbstractAprilTagPipeline}'s shared pose-estimation/multi-target/target-list logic, which does not
 * care which backend produced the detections.
 *
 * <p>Backend-specific tunables (e.g. the CPU detector's {@code decimate}/{@code blur}, and each
 * backend's own {@code refineEdges} - the two aren't interchangeable: the Vulkan backend has no
 * pre-blur stage at all, and its decimation is a separate, independently-configurable field, not
 * the CPU detector's {@code decimate}) stay on the concrete subclasses, not here.
 */
public class AprilTagPipelineSettingsBase extends AdvancedPipelineSettings {
    public AprilTagFamily tagFamily = AprilTagFamily.kTag36h11;
    public int hammingDist = 0;
    public int decisionMargin = 35;
    public int numIterations = 40;
    public boolean doMultiTarget = false;
    public boolean doSingleTargetAlways = false;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tagFamily == null) ? 0 : tagFamily.hashCode());
        result = prime * result + hammingDist;
        result = prime * result + decisionMargin;
        result = prime * result + numIterations;
        result = prime * result + (doMultiTarget ? 1231 : 1237);
        result = prime * result + (doSingleTargetAlways ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        AprilTagPipelineSettingsBase other = (AprilTagPipelineSettingsBase) obj;
        if (tagFamily != other.tagFamily) return false;
        if (hammingDist != other.hammingDist) return false;
        if (decisionMargin != other.decisionMargin) return false;
        if (numIterations != other.numIterations) return false;
        if (doMultiTarget != other.doMultiTarget) return false;
        if (doSingleTargetAlways != other.doSingleTargetAlways) return false;
        return true;
    }
}

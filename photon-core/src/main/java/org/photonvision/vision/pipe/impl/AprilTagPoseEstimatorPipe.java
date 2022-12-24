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

package org.photonvision.vision.pipe.impl;

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.apriltag.AprilTagPoseEstimator;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagPoseEstimatorPipe
        extends CVPipe<AprilTagDetection, AprilTagPoseEstimate, AprilTagPoseEstimator.Config> {
    private final AprilTagPoseEstimator m_poseEstimator =
            new AprilTagPoseEstimator(new AprilTagPoseEstimator.Config(0, 0, 0, 0, 0));

    boolean useNativePoseEst;

    public AprilTagPoseEstimatorPipe() {
        super();
    }

    @Override
    protected AprilTagPoseEstimate process(AprilTagDetection in) {
        // TODO don't hardcode # iters
        return m_poseEstimator.estimateOrthogonalIteration(in, 50);
    }

    @Override
    public void setParams(AprilTagPoseEstimator.Config newParams) {
        if (this.params != newParams) {
            m_poseEstimator.setConfig(newParams);
        }

        super.setParams(newParams);
    }

    public void setNativePoseEstimationEnabled(boolean enabled) {
        this.useNativePoseEst = enabled;
    }

    public static class AprilTagPoseEstimatorPipeParams {}
}

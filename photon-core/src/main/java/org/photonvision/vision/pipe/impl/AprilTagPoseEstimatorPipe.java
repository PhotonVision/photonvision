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
import edu.wpi.first.apriltag.AprilTagPoseEstimator.Config;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagPoseEstimatorPipe
        extends CVPipe<
                AprilTagDetection,
                AprilTagPoseEstimate,
                AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams> {
    private final AprilTagPoseEstimator m_poseEstimator =
            new AprilTagPoseEstimator(new AprilTagPoseEstimator.Config(0, 0, 0, 0, 0));

    boolean useNativePoseEst;

    public AprilTagPoseEstimatorPipe() {
        super();
    }

    @Override
    protected AprilTagPoseEstimate process(AprilTagDetection in) {
        return m_poseEstimator.estimateOrthogonalIteration(in, params.nIters);
    }

    @Override
    public void setParams(AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams newParams) {
        if (this.params == null || !this.params.equals(newParams)) {
            m_poseEstimator.setConfig(newParams.config);
        }

        super.setParams(newParams);
    }

    public void setNativePoseEstimationEnabled(boolean enabled) {
        this.useNativePoseEst = enabled;
    }

    public static class AprilTagPoseEstimatorPipeParams {
        final AprilTagPoseEstimator.Config config;
        final int nIters;

        public AprilTagPoseEstimatorPipeParams(Config config, int nIters) {
            this.config = config;
            this.nIters = nIters;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((config == null) ? 0 : config.hashCode());
            result = prime * result + nIters;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            AprilTagPoseEstimatorPipeParams other = (AprilTagPoseEstimatorPipeParams) obj;
            if (config == null) {
                if (other.config != null) return false;
            } else if (!config.equals(other.config)) return false;
            return nIters == other.nIters;
        }
    }
}

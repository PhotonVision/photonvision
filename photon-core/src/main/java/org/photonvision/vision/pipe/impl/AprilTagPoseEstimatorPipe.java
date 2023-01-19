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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.photonvision.vision.calibration.Calib3dorFisheye;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
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

    MatOfPoint2f temp = new MatOfPoint2f();

    @Override
    protected AprilTagPoseEstimate process(AprilTagDetection in) {
        // Save the corner points of our detection to an array
        Point corners[] = new Point[4];
        for (int i = 0; i < 4; i++) {
            corners[i] = new Point(in.getCornerX(i), in.getCornerY(i));
        }
        // And shove into our matofpoints
        temp.fromArray(corners);

        // Probably overwrites what was in temp before. I hope
        Calib3dorFisheye.undistortPoints(
                temp,
                temp,
                params.calibration.getCameraIntrinsicsMat(),
                params.calibration.getDistCoeffsMat());

        // Save out undistorted corners
        corners = temp.toArray();

        // Apriltagdetection expects an array in form [x1 y1 x2 y2 ...]
        var fixedCorners = new double[8];
        for (int i = 0; i < 4; i++) {
            fixedCorners[i * 2] = corners[i].x;
            fixedCorners[i * 2 + 1] = corners[i].y;
        }

        // Create a new Detection with the fixed corners
        var corrected =
                new AprilTagDetection(
                        in.getFamily(),
                        in.getId(),
                        in.getHamming(),
                        in.getDecisionMargin(),
                        in.getHomography(),
                        in.getCenterX(),
                        in.getCenterY(),
                        fixedCorners);

        return m_poseEstimator.estimateOrthogonalIteration(corrected, params.nIters);
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
        final CameraCalibrationCoefficients calibration;
        final int nIters;

        public AprilTagPoseEstimatorPipeParams(
                Config config, CameraCalibrationCoefficients cal, int nIters) {
            this.config = config;
            this.nIters = nIters;
            this.calibration = cal;
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
            if (nIters != other.nIters) return false;
            return true;
        }
    }
}

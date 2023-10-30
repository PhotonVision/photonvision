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

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.estimation.TargetModel;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.targeting.MultiTargetPNPResults;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TrackedTarget;

/** Estimate the camera pose given multiple Apriltag observations */
public class MultiTargetPNPPipe
        extends CVPipe<
                List<TrackedTarget>, MultiTargetPNPResults, MultiTargetPNPPipe.MultiTargetPNPPipeParams> {
    private static final Logger logger = new Logger(MultiTargetPNPPipe.class, LogGroup.VisionModule);

    private boolean hasWarned = false;

    @Override
    protected MultiTargetPNPResults process(List<TrackedTarget> targetList) {
        if (params.cameraCoefficients == null
                || params.cameraCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCoefficients.getDistCoeffsMat() == null) {
            if (!hasWarned) {
                logger.warn(
                        "Cannot perform solvePNP an uncalibrated camera! Please calibrate this resolution...");
                hasWarned = true;
            }
            return new MultiTargetPNPResults();
        }

        return calculateCameraInField(targetList);
    }

    private MultiTargetPNPResults calculateCameraInField(List<TrackedTarget> targetList) {
        // Find tag IDs that exist in the tag layout
        var tagIDsUsed = new ArrayList<Integer>();
        for (var target : targetList) {
            int id = target.getFiducialId();
            if (params.atfl.getTagPose(id).isPresent()) tagIDsUsed.add(id);
        }

        // Only run with multiple targets
        if (tagIDsUsed.size() < 2) {
            return new MultiTargetPNPResults();
        }

        var estimatedPose =
                VisionEstimation.estimateCamPosePNP(
                        params.cameraCoefficients.cameraIntrinsics.getAsWpilibMat(),
                        params.cameraCoefficients.distCoeffs.getAsWpilibMat(),
                        TrackedTarget.simpleFromTrackedTargets(targetList),
                        params.atfl,
                        params.targetModel);

        return new MultiTargetPNPResults(estimatedPose, tagIDsUsed);
    }

    public static class MultiTargetPNPPipeParams {
        private final CameraCalibrationCoefficients cameraCoefficients;
        private final AprilTagFieldLayout atfl;
        private final TargetModel targetModel;

        public MultiTargetPNPPipeParams(
                CameraCalibrationCoefficients cameraCoefficients,
                AprilTagFieldLayout atfl,
                TargetModel targetModel) {
            this.cameraCoefficients = cameraCoefficients;
            this.atfl = atfl;
            this.targetModel = targetModel;
        }
    }
}

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

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose3d;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.targeting.MultiTargetPNPResults;
import org.photonvision.targeting.TargetCorner;
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

        return calculateTargetPose(targetList);
    }

    private MultiTargetPNPResults calculateTargetPose(List<TrackedTarget> targetList) {
        if (params.cameraCoefficients == null
                || params.cameraCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCoefficients.getDistCoeffsMat() == null) {
            return new MultiTargetPNPResults();
        }

        var visCorners = new ArrayList<TargetCorner>();
        var knownVisTags = new ArrayList<AprilTag>();
        var tagIDsUsed = new ArrayList<Integer>();
        for (var target : targetList) {
            for (var corner : target.getTargetCorners()) {
                visCorners.add(new TargetCorner(corner.x, corner.y));
            }
            Pose3d tagPose = params.atfl.getTagPose(target.getFiducialId()).get();

            // actual layout poses of visible tags -- not exposed, so have to recreate
            knownVisTags.add(new AprilTag(target.getFiducialId(), tagPose));
            tagIDsUsed.add(target.getFiducialId());
        }
        var ret = new MultiTargetPNPResults();
        ret.estimatedPose =
                VisionEstimation.estimateCamPosePNP(
                        params.cameraCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCoefficients.getDistCoeffsMat(),
                        visCorners,
                        knownVisTags);
        ret.fiducialIDsUsed = tagIDsUsed;
        return ret;
    }

    public static class MultiTargetPNPPipeParams {
        private final CameraCalibrationCoefficients cameraCoefficients;
        private final AprilTagFieldLayout atfl;

        public MultiTargetPNPPipeParams(
                CameraCalibrationCoefficients cameraCoefficients, AprilTagFieldLayout atfl) {
            this.cameraCoefficients = cameraCoefficients;
            this.atfl = atfl;
        }
    }
}

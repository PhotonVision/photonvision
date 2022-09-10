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

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class SolvePNPPipe
        extends CVPipe<List<TrackedTarget>, List<TrackedTarget>, SolvePNPPipe.SolvePNPPipeParams> {
    private static final Logger logger = new Logger(SolvePNPPipe.class, LogGroup.VisionModule);

    private final MatOfPoint2f imagePoints = new MatOfPoint2f();

    private boolean hasWarned = false;

    @Override
    protected List<TrackedTarget> process(List<TrackedTarget> targetList) {
        if (params.cameraCoefficients == null
                || params.cameraCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCoefficients.getCameraExtrinsicsMat() == null) {
            if (!hasWarned) {
                logger.warn(
                        "Cannot perform solvePNP an uncalibrated camera! Please calibrate this resolution...");
                hasWarned = true;
            }
            return targetList;
        }

        for (var target : targetList) {
            calculateTargetPose(target);
        }
        return targetList;
    }

    private void calculateTargetPose(TrackedTarget target) {
        Transform2d targetPose;

        var corners = target.getTargetCorners();
        if (corners == null
                || corners.isEmpty()
                || params.cameraCoefficients == null
                || params.cameraCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCoefficients.getCameraExtrinsicsMat() == null) {
            return;
        }
        this.imagePoints.fromList(corners);

        var rVec = new Mat();
        var tVec = new Mat();
        try {
            Calib3d.solvePnP(
                    params.targetModel.getRealWorldTargetCoordinates(),
                    imagePoints,
                    params.cameraCoefficients.getCameraIntrinsicsMat(),
                    params.cameraCoefficients.getCameraExtrinsicsMat(),
                    rVec,
                    tVec);
        } catch (Exception e) {
            logger.error("Exception when attempting solvePnP!", e);
            return;
        }

        target.setCameraRelativeTvec(tVec);
        target.setCameraRelativeRvec(rVec);

        targetPose = correctLocationForCameraPitch(tVec, rVec, params.cameraPitchAngle);

        target.setCameraToTarget(targetPose);
    }

    Mat rotationMatrix = new Mat();
    Mat inverseRotationMatrix = new Mat();
    Mat pzeroWorld = new Mat();
    Mat kMat = new Mat();
    Mat scaledTvec;

    @SuppressWarnings("DuplicatedCode") // yes I know we have another solvePNP pipe
    private Transform2d correctLocationForCameraPitch(
            Mat tVec, Mat rVec, Rotation2d cameraPitchAngle) {
        // Algorithm from team 5190 Green Hope Falcons. Can also be found in Ligerbot's vision
        // whitepaper
        var tiltAngle = cameraPitchAngle.getRadians();

        // the left/right distance to the target, unchanged by tilt.
        var x = tVec.get(0, 0)[0];

        // Z distance in the flat plane is given by
        // Z_field = z cos theta + y sin theta.
        // Z is the distance "out" of the camera (straight forward).
        var zField = tVec.get(2, 0)[0] * Math.cos(tiltAngle) + tVec.get(1, 0)[0] * Math.sin(tiltAngle);

        Calib3d.Rodrigues(rVec, rotationMatrix);
        Core.transpose(rotationMatrix, inverseRotationMatrix);

        scaledTvec = matScale(tVec, -1);

        Core.gemm(inverseRotationMatrix, scaledTvec, 1, kMat, 0, pzeroWorld);
        scaledTvec.release();

        var angle2 = Math.atan2(pzeroWorld.get(0, 0)[0], pzeroWorld.get(2, 0)[0]);

        // target rotation is the rotation of the target relative to straight ahead. this number
        // should be unchanged if the robot purely translated left/right.
        var targetRotation = -angle2; // radians

        // We want a vector that is X forward and Y left.
        // We have a Z_field (out of the camera projected onto the field), and an X left/right.
        // so Z_field becomes X, and X becomes Y

        var targetLocation = new Translation2d(zField, -x);
        return new Transform2d(targetLocation, new Rotation2d(targetRotation));
    }

    /**
     * Element-wise scale a matrix by a given factor
     *
     * @param src the source matrix
     * @param factor by how much to scale each element
     * @return the scaled matrix
     */
    @SuppressWarnings("SameParameterValue")
    private static Mat matScale(Mat src, double factor) {
        Mat dst = new Mat(src.rows(), src.cols(), src.type());
        Scalar s = new Scalar(factor);
        Core.multiply(src, s, dst);
        return dst;
    }

    public static class SolvePNPPipeParams {
        private final CameraCalibrationCoefficients cameraCoefficients;
        private final Rotation2d cameraPitchAngle;
        private final TargetModel targetModel;

        public SolvePNPPipeParams(
                CameraCalibrationCoefficients cameraCoefficients,
                Rotation2d cameraPitchAngle,
                TargetModel targetModel) {
            this.cameraCoefficients = cameraCoefficients;
            this.cameraPitchAngle = cameraPitchAngle;
            this.targetModel = targetModel;
        }
    }
}

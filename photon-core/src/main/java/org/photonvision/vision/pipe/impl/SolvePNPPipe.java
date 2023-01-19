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

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.Calib3dorFisheye;
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
                || params.cameraCoefficients.getDistCoeffsMat() == null) {
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
        var corners = target.getTargetCorners();
        if (corners == null
                || corners.isEmpty()
                || params.cameraCoefficients == null
                || params.cameraCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCoefficients.getDistCoeffsMat() == null) {
            return;
        }
        this.imagePoints.fromList(corners);

        var rVec = new Mat();
        var tVec = new Mat();
        try {
            Calib3dorFisheye.solvePnP(
                    params.targetModel.getRealWorldTargetCoordinates(),
                    imagePoints,
                    params.cameraCoefficients.getCameraIntrinsicsMat(),
                    params.cameraCoefficients.getDistCoeffsMat(),
                    rVec,
                    tVec);
        } catch (Exception e) {
            logger.error("Exception when attempting solvePnP!", e);
            return;
        }

        target.setCameraRelativeTvec(tVec);
        target.setCameraRelativeRvec(rVec);

        Translation3d translation =
                new Translation3d(tVec.get(0, 0)[0], tVec.get(1, 0)[0], tVec.get(2, 0)[0]);
        Rotation3d rotation =
                new Rotation3d(
                        VecBuilder.fill(rVec.get(0, 0)[0], rVec.get(1, 0)[0], rVec.get(2, 0)[0]),
                        Core.norm(rVec));

        Pose3d targetPose = MathUtils.convertOpenCVtoPhotonPose(new Transform3d(translation, rotation));
        target.setBestCameraToTarget3d(
                new Transform3d(targetPose.getTranslation(), targetPose.getRotation()));
        target.setAltCameraToTarget3d(new Transform3d());
    }

    Mat rotationMatrix = new Mat();
    Mat inverseRotationMatrix = new Mat();
    Mat pzeroWorld = new Mat();
    Mat kMat = new Mat();
    Mat scaledTvec;

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
        private final TargetModel targetModel;

        public SolvePNPPipeParams(
                CameraCalibrationCoefficients cameraCoefficients, TargetModel targetModel) {
            this.cameraCoefficients = cameraCoefficients;
            this.targetModel = targetModel;
        }
    }
}

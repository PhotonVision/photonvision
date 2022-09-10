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
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class SolvePNPAprilTagsPipe
        extends CVPipe<
                List<TrackedTarget>,
                List<TrackedTarget>,
                SolvePNPAprilTagsPipe.SolvePNPAprilTagsPipeParams> {
    private static final Logger logger =
            new Logger(SolvePNPAprilTagsPipe.class, LogGroup.VisionModule);

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

        var rVecs = new ArrayList<Mat>();
        var tVecs = new ArrayList<Mat>();
        var rVec = new Mat();
        var tVec = new Mat();
        try {
            Calib3d.solvePnPGeneric(
                    params.targetModel.getRealWorldTargetCoordinates(),
                    imagePoints,
                    params.cameraCoefficients.getCameraIntrinsicsMat(),
                    params.cameraCoefficients.getCameraExtrinsicsMat(),
                    rVecs,
                    tVecs,
                    false,
                    Calib3d.SOLVEPNP_IPPE_SQUARE);
        } catch (Exception e) {
            logger.error("Exception when attempting solvePnP!", e);
            return;
        }

        tVec = tVecs.get(0);
        rVec = rVecs.get(0);
        target.setCameraRelativeTvec(tVec);
        target.setCameraRelativeRvec(rVec);

        target.setCameraToTarget3d(calculate3dTransform(tVec, rVec));

        // Pose in the flat, top down field view
        targetPose =
                correctLocationForCameraPitch(target.getCameraToTarget3d(), params.cameraPitchAngle);
        target.setCameraToTarget(targetPose);
    }

    private Transform3d calculate3dTransform(Mat tvec, Mat rvec) {
        Translation3d translation =
                new Translation3d(tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]);
        Rotation3d rotation =
                new Rotation3d(
                        VecBuilder.fill(rvec.get(0, 0)[0], rvec.get(1, 0)[0], rvec.get(2, 0)[0]),
                        Core.norm(rvec));

        var ocvPose = new Pose3d(translation, rotation);

        // SolvePNP is in EDN, we want NWU (north-west-up)
        var NWU = MathUtils.EDNtoNWU(ocvPose);
        var ret = new Transform3d(NWU.getTranslation(), NWU.getRotation());

        {
            //            System.out.println(
            //                    ret.getTranslation()
            //                            + String.format(
            //                            " Angle: X %.2f Y %.2f Z %.2f",
            //                            ret.getRotation().getX(), ret.getRotation().getY(),
            // ret.getRotation().getZ()));
            // System.out.println("Axis " + Arrays.toString(ret.getRotation().getAxis().getData()) + "
            // angle " + ret.getRotation().getAngle());
        }

        return ret;
    }

    private Transform2d correctLocationForCameraPitch(
            Transform3d cameraToTarget3d, Rotation2d cameraPitch) {
        Pose3d pose = new Pose3d(cameraToTarget3d.getTranslation(), cameraToTarget3d.getRotation());

        // We want the pose as seen by a person at the same pose as the camera, but facing
        // forward instead of pitched up
        Pose3d poseRotatedByCamAngle =
                pose.transformBy(
                        new Transform3d(new Translation3d(), new Rotation3d(0, -cameraPitch.getRadians(), 0)));

        // The pose2d from the flattened coordinate system is just the X/Y components of the 3d pose
        // and the rotation about the Z axis (which is up in the camera/field frame)
        return new Transform2d(
                new Translation2d(poseRotatedByCamAngle.getX(), poseRotatedByCamAngle.getY()),
                new Rotation2d(poseRotatedByCamAngle.getRotation().getZ()));
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

    public static class SolvePNPAprilTagsPipeParams {
        private final CameraCalibrationCoefficients cameraCoefficients;
        private final Rotation2d cameraPitchAngle;
        private final TargetModel targetModel;

        public SolvePNPAprilTagsPipeParams(
                CameraCalibrationCoefficients cameraCoefficients,
                Rotation2d cameraPitchAngle,
                TargetModel targetModel) {
            this.cameraCoefficients = cameraCoefficients;
            this.cameraPitchAngle = cameraPitchAngle;
            this.targetModel = targetModel;
        }
    }
}

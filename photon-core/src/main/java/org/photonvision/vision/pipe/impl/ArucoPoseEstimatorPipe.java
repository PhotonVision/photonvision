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

import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class ArucoPoseEstimatorPipe
        extends CVPipe<
                ArucoDetectionResult,
                AprilTagPoseEstimate,
                ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams>
        implements Releasable {
    // image points of marker corners
    private final MatOfPoint2f imagePoints = new MatOfPoint2f(Mat.zeros(4, 1, CvType.CV_32FC2));
    // rvec/tvec estimations from solvepnp
    private final List<Mat> rvecs = new ArrayList<>();
    private final List<Mat> tvecs = new ArrayList<>();
    // unused parameters
    private final Mat rvec = Mat.zeros(3, 1, CvType.CV_32F);
    private final Mat tvec = Mat.zeros(3, 1, CvType.CV_32F);
    // reprojection error of solvepnp estimations
    private final Mat reprojectionErrors = Mat.zeros(2, 1, CvType.CV_32F);

    // Tag corner locations in object space - order matters for ippe_square
    MatOfPoint3f objectPoints = new MatOfPoint3f();

    private final int kNaNRetries = 1;

    private Translation3d tvecToTranslation3d(Mat mat) {
        double[] tvec = new double[3];
        mat.get(0, 0, tvec);
        return new Translation3d(tvec[0], tvec[1], tvec[2]);
    }

    private Rotation3d rvecToRotation3d(Mat mat) {
        double[] rvec = new double[3];
        mat.get(0, 0, rvec);
        return new Rotation3d(VecBuilder.fill(rvec[0], rvec[1], rvec[2]));
    }

    @Override
    protected AprilTagPoseEstimate process(ArucoDetectionResult in) {
        // We receive 2d corners as (BL, BR, TR, TL) but we want (BR, BL, TL, TR)
        double[] xCorn = in.getXCorners();
        double[] yCorn = in.getYCorners();
        imagePoints.put(0, 0, new float[] {(float) xCorn[1], (float) yCorn[1]});
        imagePoints.put(1, 0, new float[] {(float) xCorn[0], (float) yCorn[0]});
        imagePoints.put(2, 0, new float[] {(float) xCorn[3], (float) yCorn[3]});
        imagePoints.put(3, 0, new float[] {(float) xCorn[2], (float) yCorn[2]});

        float[] reprojErrors = new float[2];
        // very rarely the solvepnp solver returns NaN results, so we retry with slight noise added
        for (int i = 0; i < kNaNRetries + 1; i++) {
            // SolvePnP with SOLVEPNP_IPPE_SQUARE solver
            Calib3d.solvePnPGeneric(
                    objectPoints,
                    imagePoints,
                    params.calibration.getCameraIntrinsicsMat(),
                    params.calibration.getDistCoeffsMat(),
                    rvecs,
                    tvecs,
                    false,
                    Calib3d.SOLVEPNP_IPPE_SQUARE,
                    rvec,
                    tvec,
                    reprojectionErrors);

            // check if we got a NaN result
            reprojectionErrors.get(0, 0, reprojErrors);
            if (!Double.isNaN(reprojErrors[0])) break;
            else { // add noise and retry
                double[] br = imagePoints.get(0, 0);
                br[0] -= 0.001;
                br[1] -= 0.001;
                imagePoints.put(0, 0, br);
            }
        }

        // create AprilTagPoseEstimate with results
        if (tvecs.isEmpty())
            return new AprilTagPoseEstimate(new Transform3d(), new Transform3d(), 0, 0);
        return new AprilTagPoseEstimate(
                new Transform3d(tvecToTranslation3d(tvecs.get(0)), rvecToRotation3d(rvecs.get(0))),
                new Transform3d(tvecToTranslation3d(tvecs.get(1)), rvecToRotation3d(rvecs.get(1))),
                reprojErrors[0],
                reprojErrors[1]);
    }

    @Override
    public void setParams(ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams newParams) {
        // exact equality check OK here, the number shouldn't change
        if (this.params == null || this.params.tagSize != newParams.tagSize) {
            var tagSize = newParams.tagSize;

            // This order is relevant for SOLVEPNP_IPPE_SQUARE
            // The expected 2d correspondences with a tag facing the camera would be (BR, BL, TL, TR)
            objectPoints.fromArray(
                    new Point3(-tagSize / 2, tagSize / 2, 0),
                    new Point3(tagSize / 2, tagSize / 2, 0),
                    new Point3(tagSize / 2, -tagSize / 2, 0),
                    new Point3(-tagSize / 2, -tagSize / 2, 0));
        }

        super.setParams(newParams);
    }

    @Override
    public void release() {
        imagePoints.release();
        for (var m : rvecs) m.release();
        rvecs.clear();
        for (var m : tvecs) m.release();
        tvecs.clear();
        rvec.release();
        tvec.release();
        reprojectionErrors.release();
    }

    public static class ArucoPoseEstimatorPipeParams {
        final CameraCalibrationCoefficients calibration;
        final double tagSize;

        // object vertices defined by tag size

        public ArucoPoseEstimatorPipeParams(CameraCalibrationCoefficients cal, double tagSize) {
            this.calibration = cal;
            this.tagSize = tagSize;
        }
    }
}

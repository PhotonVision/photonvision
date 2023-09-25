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
import org.photonvision.vision.pipe.CVPipe;

public class ArucoPoseEstimatorPipe
        extends CVPipe<
                ArucoDetectionResult,
                AprilTagPoseEstimate,
                ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams> {
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

    private final int kNaNRetries = 1;

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
        // very rarely the IPPE_SQUARE solver returns NaN results, so we retry with slight noise added
        for (int i = 0; i < kNaNRetries + 1; i++) {
            // SolvePnP with SOLVEPNP_IPPE_SQUARE solver
            Calib3d.solvePnPGeneric(
                    params.objectPoints,
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
        double[] tvec1 = new double[3];
        double[] tvec2 = new double[3];
        tvecs.get(0).get(0, 0, tvec1);
        tvecs.get(1).get(0, 0, tvec2);
        var trl1 = new Translation3d(tvec1[0], tvec1[1], tvec1[2]);
        var trl2 = new Translation3d(tvec2[0], tvec2[1], tvec2[2]);
        double[] rvec1 = new double[3];
        double[] rvec2 = new double[3];
        rvecs.get(0).get(0, 0, rvec1);
        rvecs.get(1).get(0, 0, rvec2);
        var rot1 = new Rotation3d(VecBuilder.fill(rvec1[0], rvec1[1], rvec1[2]));
        var rot2 = new Rotation3d(VecBuilder.fill(rvec2[0], rvec2[1], rvec2[2]));

        return new AprilTagPoseEstimate(
                new Transform3d(trl1, rot1), new Transform3d(trl2, rot2), reprojErrors[0], reprojErrors[1]);
    }

    @Override
    public void setParams(ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams newParams) {
        super.setParams(newParams);
    }

    public static class ArucoPoseEstimatorPipeParams {
        final CameraCalibrationCoefficients calibration;
        final double tagSize;
        // object vertices defined by tag size
        final MatOfPoint3f objectPoints;

        public ArucoPoseEstimatorPipeParams(CameraCalibrationCoefficients cal, double tagSize) {
            this.calibration = cal;
            this.tagSize = tagSize;

            // This order is relevant for SOLVEPNP_IPPE_SQUARE
            // The expected 2d correspondences with a tag facing the camera would be (BR, BL, TL, TR)
            objectPoints =
                    new MatOfPoint3f(
                            new Point3(-tagSize / 2, tagSize / 2, 0),
                            new Point3(tagSize / 2, tagSize / 2, 0),
                            new Point3(tagSize / 2, -tagSize / 2, 0),
                            new Point3(-tagSize / 2, -tagSize / 2, 0));
        }
    }
}

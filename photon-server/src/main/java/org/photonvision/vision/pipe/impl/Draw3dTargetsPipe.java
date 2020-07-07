/*
 * Copyright (C) 2020 Photon Vision.
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

import java.awt.*;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class Draw3dTargetsPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, Draw3dTargetsPipe.Draw3dContoursParams> {

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        for (var target : in.getRight()) {

            // draw convex hull
            var pointMat = new MatOfPoint();
            target.m_mainContour.getConvexHull().convertTo(pointMat, CvType.CV_32S);
            Imgproc.drawContours(
                    in.getLeft(), List.of(pointMat), -1, ColorHelper.colorToScalar(Color.green), 1);

            // draw approximate polygon
            var poly = target.getApproximateBoundingPolygon();
            if (poly != null) {
                poly.convertTo(pointMat, CvType.CV_32S);
                Imgproc.drawContours(
                        in.getLeft(), List.of(pointMat), -1, ColorHelper.colorToScalar(Color.blue), 2);
            }

            // Draw floor and top
            if (target.getCameraRelativeRvec() != null && target.getCameraRelativeTvec() != null) {
                var tempMat = new MatOfPoint2f();
                var jac = new Mat();
                var bottomModel = params.targetModel.getVisualizationBoxBottom();
                var topModel = params.targetModel.getVisualizationBoxTop();
                Calib3d.projectPoints(
                        bottomModel,
                        target.getCameraRelativeRvec(),
                        target.getCameraRelativeTvec(),
                        params.cameraCalibrationCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCalibrationCoefficients.getCameraExtrinsicsMat(),
                        tempMat,
                        jac);
                var bottomPoints = tempMat.toList();
                Calib3d.projectPoints(
                        topModel,
                        target.getCameraRelativeRvec(),
                        target.getCameraRelativeTvec(),
                        params.cameraCalibrationCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCalibrationCoefficients.getCameraExtrinsicsMat(),
                        tempMat,
                        jac);
                var topPoints = tempMat.toList();
                // floor, then pillers, then top
                for (int i = 0; i < bottomPoints.size(); i++) {
                    Imgproc.line(
                            in.getLeft(),
                            bottomPoints.get(i),
                            bottomPoints.get((i + 1) % (bottomPoints.size())),
                            ColorHelper.colorToScalar(Color.green),
                            3);
                }
                for (int i = 0; i < bottomPoints.size(); i++) {
                    Imgproc.line(
                            in.getLeft(),
                            bottomPoints.get(i),
                            topPoints.get(i),
                            ColorHelper.colorToScalar(Color.blue),
                            3);
                }
                for (int i = 0; i < topPoints.size(); i++) {
                    Imgproc.line(
                            in.getLeft(),
                            topPoints.get(i),
                            topPoints.get((i + 1) % (bottomPoints.size())),
                            ColorHelper.colorToScalar(Color.orange),
                            3);
                }

                jac.release();
            }
            pointMat.release();

            // draw corners
            var corners = target.getTargetCorners();
            if (corners != null && !corners.isEmpty()) {
                for (var corner : corners) {
                    Imgproc.circle(
                            in.getLeft(),
                            corner,
                            params.radius,
                            ColorHelper.colorToScalar(params.color),
                            params.radius);
                }
            }
        }

        return in.getLeft();
    }

    public static class Draw3dContoursParams {
        private final int radius = 2;
        private final Color color = Color.RED;
        private final TargetModel targetModel;
        private final CameraCalibrationCoefficients cameraCalibrationCoefficients;

        public Draw3dContoursParams(
                CameraCalibrationCoefficients cameraCalibrationCoefficients, TargetModel targetModel) {
            this.cameraCalibrationCoefficients = cameraCalibrationCoefficients;
            this.targetModel = targetModel;
        }
    }
}

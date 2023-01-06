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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class Draw3dTargetsPipe
        extends MutatingPipe<Pair<Mat, List<TrackedTarget>>, Draw3dTargetsPipe.Draw3dContoursParams> {
    Logger logger = new Logger(Draw3dTargetsPipe.class, LogGroup.VisionModule);

    @Override
    protected Void process(Pair<Mat, List<TrackedTarget>> in) {
        if (!params.shouldDraw) return null;
        if (params.cameraCalibrationCoefficients == null
                || params.cameraCalibrationCoefficients.getCameraIntrinsicsMat() == null
                || params.cameraCalibrationCoefficients.getDistCoeffsMat() == null) {
            return null;
        }

        for (var target : in.getRight()) {
            // draw convex hull
            if (params.shouldDrawHull(target)) {
                var pointMat = new MatOfPoint();
                divideMat2f(target.m_mainContour.getConvexHull(), pointMat);
                if (pointMat.size().empty()) {
                    logger.error("Convex hull is empty?");
                    logger.debug(
                            "Orig. Convex Hull: " + target.m_mainContour.getConvexHull().size().toString());
                    continue;
                }
                Imgproc.drawContours(
                        in.getLeft(), List.of(pointMat), -1, ColorHelper.colorToScalar(Color.green), 1);

                // draw approximate polygon
                var poly = target.getApproximateBoundingPolygon();
                if (poly != null) {
                    divideMat2f(poly, pointMat);
                    Imgproc.drawContours(
                            in.getLeft(), List.of(pointMat), -1, ColorHelper.colorToScalar(Color.blue), 2);
                }
                pointMat.release();
            }

            // Draw floor and top
            if (target.getCameraRelativeRvec() != null
                    && target.getCameraRelativeTvec() != null
                    && params.shouldDrawBox) {
                var tempMat = new MatOfPoint2f();

                var jac = new Mat();
                var bottomModel = params.targetModel.getVisualizationBoxBottom();
                var topModel = params.targetModel.getVisualizationBoxTop();

                Calib3d.projectPoints(
                        bottomModel,
                        target.getCameraRelativeRvec(),
                        target.getCameraRelativeTvec(),
                        params.cameraCalibrationCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCalibrationCoefficients.getDistCoeffsMat(),
                        tempMat,
                        jac);

                if (params.redistortPoints) {
                    // Distort the points so they match the image they're being overlaid on
                    distortPoints(tempMat, tempMat);
                }

                var bottomPoints = tempMat.toList();

                Calib3d.projectPoints(
                        topModel,
                        target.getCameraRelativeRvec(),
                        target.getCameraRelativeTvec(),
                        params.cameraCalibrationCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCalibrationCoefficients.getDistCoeffsMat(),
                        tempMat,
                        jac);

                if (params.redistortPoints) {
                    // Distort the points so they match the image they're being overlaid on
                    distortPoints(tempMat, tempMat);
                }
                var topPoints = tempMat.toList();

                dividePointList(bottomPoints);
                dividePointList(topPoints);

                // floor, then pillers, then top
                for (int i = 0; i < bottomPoints.size(); i++) {
                    Imgproc.line(
                            in.getLeft(),
                            bottomPoints.get(i),
                            bottomPoints.get((i + 1) % (bottomPoints.size())),
                            ColorHelper.colorToScalar(Color.green),
                            3);
                }

                // Draw X, Y and Z axis
                MatOfPoint3f pointMat = new MatOfPoint3f();
                // Those points are in opencv-land, but we are in NWU
                // NWU | EDN
                // X: Z
                // Y: -X
                // Z: -Y
                final double AXIS_LEN = 0.2;
                var list =
                        List.of(
                                new Point3(0, 0, 0),
                                new Point3(0, 0, AXIS_LEN),
                                new Point3(AXIS_LEN, 0, 0),
                                new Point3(0, AXIS_LEN, 0));
                pointMat.fromList(list);

                Calib3d.projectPoints(
                        pointMat,
                        target.getCameraRelativeRvec(),
                        target.getCameraRelativeTvec(),
                        params.cameraCalibrationCoefficients.getCameraIntrinsicsMat(),
                        params.cameraCalibrationCoefficients.getDistCoeffsMat(),
                        tempMat,
                        jac);
                var axisPoints = tempMat.toList();
                dividePointList(axisPoints);

                // Red = x, green y, blue z
                Imgproc.line(
                        in.getLeft(),
                        axisPoints.get(0),
                        axisPoints.get(2),
                        ColorHelper.colorToScalar(Color.GREEN),
                        3);
                Imgproc.line(
                        in.getLeft(),
                        axisPoints.get(0),
                        axisPoints.get(3),
                        ColorHelper.colorToScalar(Color.BLUE),
                        3);
                Imgproc.line(
                        in.getLeft(),
                        axisPoints.get(0),
                        axisPoints.get(1),
                        ColorHelper.colorToScalar(Color.RED),
                        3);

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

                tempMat.release();
                jac.release();
                pointMat.release();
            }

            // draw corners
            var corners = target.getTargetCorners();
            if (corners != null && !corners.isEmpty()) {
                for (var corner : corners) {
                    var x = corner.x / (double) params.divisor.value;
                    var y = corner.y / (double) params.divisor.value;

                    Imgproc.circle(
                            in.getLeft(),
                            new Point(x, y),
                            params.radius,
                            ColorHelper.colorToScalar(params.color),
                            params.radius);
                }
            }
        }

        return null;
    }

    private void distortPoints(MatOfPoint2f src, MatOfPoint2f dst) {
        var pointsList = src.toList();
        var dstList = new ArrayList<Point>();
        final Mat cameraMatrix = params.cameraCalibrationCoefficients.getCameraIntrinsicsMat();
        // k1, k2, p1, p2, k3
        final Mat distCoeffs = params.cameraCalibrationCoefficients.getDistCoeffsMat();
        var cx = cameraMatrix.get(0, 2)[0];
        var cy = cameraMatrix.get(1, 2)[0];
        var fx = cameraMatrix.get(0, 0)[0];
        var fy = cameraMatrix.get(1, 1)[0];
        var k1 = distCoeffs.get(0, 0)[0];
        var k2 = distCoeffs.get(0, 1)[0];
        var k3 = distCoeffs.get(0, 4)[0];
        var p1 = distCoeffs.get(0, 2)[0];
        var p2 = distCoeffs.get(0, 3)[0];

        for (Point point : pointsList) {
            // To relative coordinates <- this is the step you are missing.
            double x = (point.x - cx) / fx; // cx, cy is the center of distortion
            double y = (point.y - cy) / fy;

            double r2 = x * x + y * y; // square of the radius from center

            // Radial distorsion
            double xDistort = x * (1 + k1 * r2 + k2 * r2 * r2 + k3 * r2 * r2 * r2);
            double yDistort = y * (1 + k1 * r2 + k2 * r2 * r2 + k3 * r2 * r2 * r2);

            // Tangential distorsion
            xDistort = xDistort + (2 * p1 * x * y + p2 * (r2 + 2 * x * x));
            yDistort = yDistort + (p1 * (r2 + 2 * y * y) + 2 * p2 * x * y);

            // Back to absolute coordinates.
            xDistort = xDistort * fx + cx;
            yDistort = yDistort * fy + cy;
            dstList.add(new Point(xDistort, yDistort));
        }
        dst.fromList(dstList);
    }

    private void divideMat2f(MatOfPoint2f src, MatOfPoint dst) {
        var hull = src.toArray();
        var pointArray = new Point[hull.length];
        for (int i = 0; i < hull.length; i++) {
            var hullAtI = hull[i];
            pointArray[i] =
                    new Point(
                            hullAtI.x / (double) params.divisor.value, hullAtI.y / (double) params.divisor.value);
        }
        dst.fromArray(pointArray);
    }

    private void divideMat2f(MatOfPoint2f src, MatOfPoint2f dst) {
        var hull = src.toArray();
        var pointArray = new Point[hull.length];
        for (int i = 0; i < hull.length; i++) {
            var hullAtI = hull[i];
            pointArray[i] =
                    new Point(
                            hullAtI.x / (double) params.divisor.value, hullAtI.y / (double) params.divisor.value);
        }
        dst.fromArray(pointArray);
    }

    /** Scale a given point list by the current frame divisor. the point list is mutated! */
    private void dividePointList(List<Point> points) {
        for (var p : points) {
            p.x = p.x / (double) params.divisor.value;
            p.y = p.y / (double) params.divisor.value;
        }
    }

    public static class Draw3dContoursParams {
        public int radius = 2;
        public Color color = Color.RED;

        public final boolean shouldDraw;
        public boolean shouldDrawHull = true;
        public boolean shouldDrawBox = true;
        public final TargetModel targetModel;
        public final CameraCalibrationCoefficients cameraCalibrationCoefficients;
        public final FrameDivisor divisor;

        public boolean redistortPoints = false;

        public Draw3dContoursParams(
                boolean shouldDraw,
                CameraCalibrationCoefficients cameraCalibrationCoefficients,
                TargetModel targetModel,
                FrameDivisor divisor) {
            this.shouldDraw = shouldDraw;
            this.cameraCalibrationCoefficients = cameraCalibrationCoefficients;
            this.targetModel = targetModel;
            this.divisor = divisor;
        }

        public boolean shouldDrawHull(TrackedTarget t) {
            return !t.isFiducial() && this.shouldDrawHull;
        }
    }
}

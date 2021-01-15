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

package org.photonvision.vision.target;

import edu.wpi.first.wpilibj.geometry.Transform2d;
import java.util.HashMap;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.opencv.Releasable;

public class TrackedTarget implements Releasable {
    public final Contour m_mainContour;
    List<Contour> m_subContours; // can be empty

    private MatOfPoint2f m_approximateBoundingPolygon;

    private List<Point> m_targetCorners;

    private Point m_targetOffsetPoint;
    private Point m_robotOffsetPoint;

    private double m_pitch;
    private double m_yaw;
    private double m_area;
    private double m_skew;

    private Transform2d m_cameraToTarget = new Transform2d();

    private Mat m_cameraRelativeTvec, m_cameraRelativeRvec;

    public TrackedTarget(PotentialTarget origTarget, TargetCalculationParameters params) {
        this.m_mainContour = origTarget.m_mainContour;
        this.m_subContours = origTarget.m_subContours;
        calculateValues(params);
    }

    /**
    * Set the approximate bouding polygon.
    *
    * @param boundingPolygon List of points to copy. Not modified.
    */
    public void setApproximateBoundingPolygon(MatOfPoint2f boundingPolygon) {
        if (m_approximateBoundingPolygon == null) m_approximateBoundingPolygon = new MatOfPoint2f();
        boundingPolygon.copyTo(m_approximateBoundingPolygon);
    }

    public Point getTargetOffsetPoint() {
        return m_targetOffsetPoint;
    }

    public Point getRobotOffsetPoint() {
        return m_robotOffsetPoint;
    }

    public double getPitch() {
        return m_pitch;
    }

    public double getYaw() {
        return m_yaw;
    }

    public double getSkew() {
        return m_skew;
    }

    public double getArea() {
        return m_area;
    }

    public RotatedRect getMinAreaRect() {
        return m_mainContour.getMinAreaRect();
    }

    public MatOfPoint2f getApproximateBoundingPolygon() {
        return m_approximateBoundingPolygon;
    }

    public void calculateValues(TargetCalculationParameters params) {
        // this MUST happen in this exact order!
        m_targetOffsetPoint =
                TargetCalculations.calculateTargetOffsetPoint(
                        params.isLandscape, params.targetOffsetPointEdge, getMinAreaRect());
        m_robotOffsetPoint =
                TargetCalculations.calculateRobotOffsetPoint(
                        params.robotOffsetSinglePoint,
                        params.cameraCenterPoint,
                        params.dualOffsetValues,
                        params.robotOffsetPointMode);

        // order of this stuff doesnt matter though
        m_pitch =
                TargetCalculations.calculatePitch(
                        m_targetOffsetPoint.y, m_robotOffsetPoint.y, params.verticalFocalLength);
        m_yaw =
                TargetCalculations.calculateYaw(
                        m_targetOffsetPoint.x, m_robotOffsetPoint.x, params.horizontalFocalLength);
        m_area = m_mainContour.getMinAreaRect().size.area() / params.imageArea * 100;

        m_skew = TargetCalculations.calculateSkew(params.isLandscape, getMinAreaRect());
    }

    @Override
    public void release() {
        m_mainContour.release();
        for (var sc : m_subContours) {
            sc.release();
        }

        if (m_cameraRelativeTvec != null) m_cameraRelativeTvec.release();
        if (m_cameraRelativeRvec != null) m_cameraRelativeRvec.release();
    }

    public void setCorners(List<Point> targetCorners) {
        this.m_targetCorners = targetCorners;
    }

    public List<Point> getTargetCorners() {
        return m_targetCorners;
    }

    public boolean hasSubContours() {
        return !m_subContours.isEmpty();
    }

    public Transform2d getCameraToTarget() {
        return m_cameraToTarget;
    }

    public void setCameraToTarget(Transform2d pose) {
        this.m_cameraToTarget = pose;
    }

    public Mat getCameraRelativeTvec() {
        return m_cameraRelativeTvec;
    }

    public void setCameraRelativeTvec(Mat cameraRelativeTvec) {
        if (this.m_cameraRelativeTvec == null) m_cameraRelativeTvec = new Mat();
        cameraRelativeTvec.copyTo(this.m_cameraRelativeTvec);
    }

    public Mat getCameraRelativeRvec() {
        return m_cameraRelativeRvec;
    }

    public void setCameraRelativeRvec(Mat cameraRelativeRvec) {
        if (this.m_cameraRelativeRvec == null) m_cameraRelativeRvec = new Mat();
        cameraRelativeRvec.copyTo(this.m_cameraRelativeRvec);
    }

    public HashMap<String, Object> toHashMap() {
        //                pitch: 0,
        //                    yaw: 0,
        //                    skew: 0,
        //                    area: 0,
        //                    // 3D only
        //                    pose: {x: 0, y: 0, rot: 0},
        var ret = new HashMap<String, Object>();
        ret.put("pitch", getPitch());
        ret.put("yaw", getYaw());
        ret.put("skew", getSkew());
        ret.put("area", getArea());
        if (getCameraToTarget() != null) {
            ret.put("pose", transformToMap(getCameraToTarget()));
        }
        return ret;
    }

    private static HashMap<String, Object> transformToMap(Transform2d transform) {
        var ret = new HashMap<String, Object>();
        ret.put("x", transform.getTranslation().getX());
        ret.put("y", transform.getTranslation().getY());
        ret.put("rot", transform.getRotation().getDegrees());
        return ret;
    }

    public static class TargetCalculationParameters {
        // TargetOffset calculation values
        final boolean isLandscape;
        final TargetOffsetPointEdge targetOffsetPointEdge;

        // RobotOffset calculation values
        final RobotOffsetPointMode robotOffsetPointMode;
        final Point robotOffsetSinglePoint;
        final DualOffsetValues dualOffsetValues;

        // center point of image
        final Point cameraCenterPoint;

        // yaw calculation values
        final double horizontalFocalLength;

        // pitch calculation values
        final double verticalFocalLength;

        // area calculation values
        final double imageArea;

        public TargetCalculationParameters(
                boolean isLandscape,
                TargetOffsetPointEdge targetOffsetPointEdge,
                RobotOffsetPointMode robotOffsetPointMode,
                Point robotOffsetSinglePoint,
                DualOffsetValues dualOffsetValues,
                Point cameraCenterPoint,
                double horizontalFocalLength,
                double verticalFocalLength,
                double imageArea) {

            this.isLandscape = isLandscape;
            this.targetOffsetPointEdge = targetOffsetPointEdge;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.robotOffsetSinglePoint = robotOffsetSinglePoint;
            this.dualOffsetValues = dualOffsetValues;
            this.cameraCenterPoint = cameraCenterPoint;
            this.horizontalFocalLength = horizontalFocalLength;
            this.verticalFocalLength = verticalFocalLength;
            this.imageArea = imageArea;
        }

        public TargetCalculationParameters(
                boolean isLandscape,
                TargetOffsetPointEdge targetOffsetPointEdge,
                RobotOffsetPointMode robotOffsetPointMode,
                Point robotOffsetSinglePoint,
                DualOffsetValues dualOffsetValues,
                FrameStaticProperties frameStaticProperties) {

            this.isLandscape = isLandscape;
            this.targetOffsetPointEdge = targetOffsetPointEdge;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.robotOffsetSinglePoint = robotOffsetSinglePoint;
            this.dualOffsetValues = dualOffsetValues;

            this.cameraCenterPoint = frameStaticProperties.centerPoint;
            this.horizontalFocalLength = frameStaticProperties.horizontalFocalLength;
            this.verticalFocalLength = frameStaticProperties.verticalFocalLength;
            this.imageArea = frameStaticProperties.imageArea;
        }
    }
}

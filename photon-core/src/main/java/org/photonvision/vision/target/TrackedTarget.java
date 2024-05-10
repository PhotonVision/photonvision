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

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.math.geometry.Transform3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.opencv.Releasable;

public class TrackedTarget implements Releasable {
    public final Contour m_mainContour;
    public List<Contour> m_subContours; // can be empty

    private MatOfPoint2f m_approximateBoundingPolygon;

    private List<Point> m_targetCorners = List.of();

    private Point m_targetOffsetPoint;
    private Point m_robotOffsetPoint;

    private double m_pitch;
    private double m_yaw;
    private double m_area;
    private double m_skew;

    private Transform3d m_bestCameraToTarget3d = new Transform3d();
    private Transform3d m_altCameraToTarget3d = new Transform3d();

    private CVShape m_shape;

    private int m_fiducialId = -1;
    private double m_poseAmbiguity = -1;

    private Mat m_cameraRelativeTvec, m_cameraRelativeRvec;

    private int m_classId = -1;
    private double m_confidence = -1;

    public TrackedTarget(
            PotentialTarget origTarget, TargetCalculationParameters params, CVShape shape) {
        this.m_mainContour = origTarget.m_mainContour;
        this.m_subContours = origTarget.m_subContours;
        this.m_shape = shape;
        calculateValues(params);

        this.m_classId = origTarget.clsId;
        this.m_confidence = origTarget.confidence;
    }

    public TrackedTarget(
            AprilTagDetection tagDetection,
            AprilTagPoseEstimate tagPose,
            TargetCalculationParameters params) {
        m_targetOffsetPoint = new Point(tagDetection.getCenterX(), tagDetection.getCenterY());
        m_robotOffsetPoint = new Point();
        var yawPitch =
                TargetCalculations.calculateYawPitch(
                        params.cameraCenterPoint.x,
                        tagDetection.getCenterX(),
                        params.horizontalFocalLength,
                        params.cameraCenterPoint.y,
                        tagDetection.getCenterY(),
                        params.verticalFocalLength,
                        params.cameraCal);
        m_yaw = yawPitch.getFirst();
        m_pitch = yawPitch.getSecond();
        var bestPose = new Transform3d();
        var altPose = new Transform3d();

        if (tagPose != null) {
            if (tagPose.error1 <= tagPose.error2) {
                bestPose = tagPose.pose1;
                altPose = tagPose.pose2;
            } else {
                bestPose = tagPose.pose2;
                altPose = tagPose.pose1;
            }

            bestPose = MathUtils.convertApriltagtoOpenCV(bestPose);
            altPose = MathUtils.convertApriltagtoOpenCV(altPose);

            m_bestCameraToTarget3d = bestPose;
            m_altCameraToTarget3d = altPose;

            m_poseAmbiguity = tagPose.getAmbiguity();

            var tvec = new Mat(3, 1, CvType.CV_64FC1);
            tvec.put(
                    0,
                    0,
                    new double[] {
                        bestPose.getTranslation().getX(),
                        bestPose.getTranslation().getY(),
                        bestPose.getTranslation().getZ()
                    });
            setCameraRelativeTvec(tvec);

            // Opencv expects a 3d vector with norm = angle and direction = axis
            var rvec = new Mat(3, 1, CvType.CV_64FC1);
            MathUtils.rotationToOpencvRvec(bestPose.getRotation(), rvec);
            setCameraRelativeRvec(rvec);
        }

        double[] corners = tagDetection.getCorners();
        Point[] cornerPoints =
                new Point[] {
                    new Point(corners[0], corners[1]),
                    new Point(corners[2], corners[3]),
                    new Point(corners[4], corners[5]),
                    new Point(corners[6], corners[7])
                };
        m_targetCorners = List.of(cornerPoints);
        MatOfPoint contourMat = new MatOfPoint(cornerPoints);
        m_approximateBoundingPolygon = new MatOfPoint2f(cornerPoints);
        m_mainContour = new Contour(contourMat);
        m_area = m_mainContour.getArea() / params.imageArea * 100;
        m_fiducialId = tagDetection.getId();
        m_shape = null;

        // TODO implement skew? or just yeet
        m_skew = 0;
    }

    public TrackedTarget(List<Point> corners) {
        m_mainContour = new Contour(new MatOfPoint());
        m_mainContour.mat.fromList(List.of(new Point(0, 0), new Point(0, 1), new Point(1, 0)));
        this.setTargetCorners(corners);
        m_targetOffsetPoint = new Point();
        m_robotOffsetPoint = new Point();
    }

    /**
     * @return Returns the confidence of the detection ranging from 0 - 1.
     */
    public double getConfidence() {
        return m_confidence;
    }

    /**
     * @return O-indexed class index for the detected object.
     */
    public double getClassID() {
        return m_classId;
    }

    public TrackedTarget(
            ArucoDetectionResult result,
            AprilTagPoseEstimate tagPose,
            TargetCalculationParameters params) {
        m_targetOffsetPoint = new Point(result.getCenterX(), result.getCenterY());
        m_robotOffsetPoint = new Point();
        var yawPitch =
                TargetCalculations.calculateYawPitch(
                        params.cameraCenterPoint.x,
                        result.getCenterX(),
                        params.horizontalFocalLength,
                        params.cameraCenterPoint.y,
                        result.getCenterY(),
                        params.verticalFocalLength,
                        params.cameraCal);
        m_yaw = yawPitch.getFirst();
        m_pitch = yawPitch.getSecond();

        double[] xCorners = result.getXCorners();
        double[] yCorners = result.getYCorners();

        Point[] cornerPoints =
                new Point[] {
                    new Point(xCorners[0], yCorners[0]),
                    new Point(xCorners[1], yCorners[1]),
                    new Point(xCorners[2], yCorners[2]),
                    new Point(xCorners[3], yCorners[3])
                };
        m_targetCorners = List.of(cornerPoints);
        MatOfPoint contourMat = new MatOfPoint(cornerPoints);
        m_approximateBoundingPolygon = new MatOfPoint2f(cornerPoints);
        m_mainContour = new Contour(contourMat);
        m_area = m_mainContour.getArea() / params.imageArea * 100;
        m_fiducialId = result.getId();
        m_shape = null;

        // TODO implement skew? or just yeet
        m_skew = 0;

        var bestPose = new Transform3d();
        var altPose = new Transform3d();
        if (tagPose != null) {
            if (tagPose.error1 <= tagPose.error2) {
                bestPose = tagPose.pose1;
                altPose = tagPose.pose2;
            } else {
                bestPose = tagPose.pose2;
                altPose = tagPose.pose1;
            }

            m_bestCameraToTarget3d = bestPose;
            m_altCameraToTarget3d = altPose;

            m_poseAmbiguity = tagPose.getAmbiguity();

            var tvec = new Mat(3, 1, CvType.CV_64FC1);
            tvec.put(
                    0,
                    0,
                    new double[] {
                        bestPose.getTranslation().getX(),
                        bestPose.getTranslation().getY(),
                        bestPose.getTranslation().getZ()
                    });
            setCameraRelativeTvec(tvec);

            var rvec = new Mat(3, 1, CvType.CV_64FC1);
            MathUtils.rotationToOpencvRvec(bestPose.getRotation(), rvec);
            setCameraRelativeRvec(rvec);
        }
    }

    public void setFiducialId(int id) {
        m_fiducialId = id;
    }

    public int getFiducialId() {
        return m_fiducialId;
    }

    public void setPoseAmbiguity(double ambiguity) {
        m_poseAmbiguity = ambiguity;
    }

    public double getPoseAmbiguity() {
        return m_poseAmbiguity;
    }

    /**
     * Set the approximate bounding polygon.
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
        // this MUST happen in this exact order! (TODO: document why)
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
        var yawPitch =
                TargetCalculations.calculateYawPitch(
                        m_robotOffsetPoint.x,
                        m_targetOffsetPoint.x,
                        params.horizontalFocalLength,
                        m_robotOffsetPoint.y,
                        m_targetOffsetPoint.y,
                        params.verticalFocalLength,
                        params.cameraCal);
        m_yaw = yawPitch.getFirst();
        m_pitch = yawPitch.getSecond();

        m_area = m_mainContour.getMinAreaRect().size.area() / params.imageArea * 100;

        m_skew = TargetCalculations.calculateSkew(params.isLandscape, getMinAreaRect());
    }

    @Override
    public void release() {
        m_mainContour.release();

        // TODO how can this check fail?
        if (m_subContours != null) {
            for (var sc : m_subContours) {
                sc.release();
            }
        }

        if (m_cameraRelativeTvec != null) m_cameraRelativeTvec.release();
        if (m_cameraRelativeRvec != null) m_cameraRelativeRvec.release();
    }

    public void setTargetCorners(List<Point> targetCorners) {
        this.m_targetCorners = targetCorners;
    }

    public List<Point> getTargetCorners() {
        return m_targetCorners;
    }

    public boolean hasSubContours() {
        return !m_subContours.isEmpty();
    }

    public Transform3d getBestCameraToTarget3d() {
        return m_bestCameraToTarget3d;
    }

    public Transform3d getAltCameraToTarget3d() {
        return m_altCameraToTarget3d;
    }

    public void setBestCameraToTarget3d(Transform3d pose) {
        this.m_bestCameraToTarget3d = pose;
    }

    public void setAltCameraToTarget3d(Transform3d pose) {
        this.m_altCameraToTarget3d = pose;
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

    public CVShape getShape() {
        return m_shape;
    }

    public void setShape(CVShape shape) {
        this.m_shape = shape;
    }

    public HashMap<String, Object> toHashMap() {
        var ret = new HashMap<String, Object>();
        ret.put("pitch", getPitch());
        ret.put("yaw", getYaw());
        ret.put("skew", getSkew());
        ret.put("area", getArea());
        ret.put("ambiguity", getPoseAmbiguity());
        ret.put("confidence", m_confidence);
        ret.put("classId", m_classId);

        var bestCameraToTarget3d = getBestCameraToTarget3d();
        if (bestCameraToTarget3d != null) {
            ret.put("pose", SerializationUtils.transformToHashMap(bestCameraToTarget3d));
        }
        ret.put("fiducialId", getFiducialId());
        return ret;
    }

    public boolean isFiducial() {
        return this.m_fiducialId >= 0;
    }

    public static List<PhotonTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<PhotonTrackedTarget>();
        for (var t : targets) {
            var minAreaRectCorners = new ArrayList<TargetCorner>();
            var detectedCorners = new ArrayList<TargetCorner>();
            {
                var points = new Point[4];
                t.getMinAreaRect().points(points);
                for (int i = 0; i < 4; i++) {
                    minAreaRectCorners.add(new TargetCorner(points[i].x, points[i].y));
                }
            }
            {
                var points = t.getTargetCorners();
                for (Point point : points) {
                    detectedCorners.add(new TargetCorner(point.x, point.y));
                }
            }

            ret.add(
                    new PhotonTrackedTarget(
                            t.getYaw(),
                            t.getPitch(),
                            t.getArea(),
                            t.getSkew(),
                            t.getFiducialId(),
                            t.getBestCameraToTarget3d(),
                            t.getAltCameraToTarget3d(),
                            t.getPoseAmbiguity(),
                            minAreaRectCorners,
                            detectedCorners));
        }
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

        // Camera calibration, null if not calibrated
        final CameraCalibrationCoefficients cameraCal;

        public TargetCalculationParameters(
                boolean isLandscape,
                TargetOffsetPointEdge targetOffsetPointEdge,
                RobotOffsetPointMode robotOffsetPointMode,
                Point robotOffsetSinglePoint,
                DualOffsetValues dualOffsetValues,
                Point cameraCenterPoint,
                double horizontalFocalLength,
                double verticalFocalLength,
                double imageArea,
                CameraCalibrationCoefficients cal) {

            this.isLandscape = isLandscape;
            this.targetOffsetPointEdge = targetOffsetPointEdge;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.robotOffsetSinglePoint = robotOffsetSinglePoint;
            this.dualOffsetValues = dualOffsetValues;
            this.cameraCenterPoint = cameraCenterPoint;
            this.horizontalFocalLength = horizontalFocalLength;
            this.verticalFocalLength = verticalFocalLength;
            this.imageArea = imageArea;
            this.cameraCal = cal;
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
            this.cameraCal = frameStaticProperties.cameraCalibration;
        }
    }
}

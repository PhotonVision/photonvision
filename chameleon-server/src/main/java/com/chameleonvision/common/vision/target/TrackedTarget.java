package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.opencv.Contour;
import com.chameleonvision.common.vision.opencv.Releasable;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

// TODO: banks fix
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

    private Pose2d m_robotRelativePose;

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

    public double getArea() {
        return m_area;
    }

    public RotatedRect getMinAreaRect() {
        return m_mainContour.getMinAreaRect();
    }

    public MatOfPoint2f getApproximateBoundingPolygon() {
        return m_approximateBoundingPolygon;
    }

    private void calculateTargetOffsetPoint(boolean isLandscape, TargetOffsetPointEdge offsetRegion) {
        Point[] vertices = new Point[4];

        var minAreaRect = getMinAreaRect();
        minAreaRect.points(vertices);

        Point bl = getMiddle(vertices[0], vertices[1]);
        Point tl = getMiddle(vertices[1], vertices[2]);
        Point tr = getMiddle(vertices[2], vertices[3]);
        Point br = getMiddle(vertices[3], vertices[0]);
        boolean orientation;
        if (isLandscape) {
            orientation = minAreaRect.size.width > minAreaRect.size.height;
        } else {
            orientation = minAreaRect.size.width < minAreaRect.size.height;
        }

        Point resultPoint = minAreaRect.center;
        switch (offsetRegion) {
            case Top:
                {
                    resultPoint = orientation ? tl : tr;
                    break;
                }
            case Bottom:
                {
                    resultPoint = orientation ? br : bl;
                    break;
                }
            case Left:
                {
                    resultPoint = orientation ? bl : tl;
                    break;
                }
            case Right:
                {
                    resultPoint = orientation ? tr : br;
                    break;
                }
        }
        m_targetOffsetPoint = resultPoint;
    }

    private void calculateRobotOffsetPoint(
            Point offsetPoint,
            Point camCenterPoint,
            DoubleCouple offsetEquationValues,
            RobotOffsetPointMode offsetMode) {
        Point resultPoint = new Point();
        switch (offsetMode) {
            case None:
                resultPoint = camCenterPoint;
                break;
            case Single:
                if (offsetPoint.x == 0 && offsetPoint.y == 0) {
                    resultPoint = camCenterPoint;
                } else {
                    resultPoint = offsetPoint;
                }
                break;
            case Dual:
                resultPoint.x =
                        (offsetPoint.x - offsetEquationValues.getFirst()) / offsetEquationValues.getSecond();
                resultPoint.y =
                        (offsetPoint.y * offsetEquationValues.getSecond()) + offsetEquationValues.getFirst();
                break;
        }

        m_robotOffsetPoint = resultPoint;
    }

    private void calculatePitch(double verticalFocalLength) {
        double contourCenterY = m_mainContour.getCenterPoint().y;
        double targetCenterY = m_targetOffsetPoint.y;
        m_pitch =
                -FastMath.toDegrees(FastMath.atan((contourCenterY - targetCenterY) / verticalFocalLength));
    }

    private void calculateYaw(double horizontalFocalLength) {
        double contourCenterX = m_mainContour.getCenterPoint().x;
        double targetCenterX = m_targetOffsetPoint.x;
        m_yaw =
                FastMath.toDegrees(FastMath.atan((contourCenterX - targetCenterX) / horizontalFocalLength));
    }

    private void calculateArea(double imageArea) {
        m_area = m_mainContour.getMinAreaRect().size.area() / imageArea;
    }

    private Point getMiddle(Point p1, Point p2) {
        return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
    }

    public void calculateValues(TargetCalculationParameters params) {
        // this MUST happen in this exact order!
        calculateTargetOffsetPoint(params.isLandscape, params.targetOffsetPointEdge);
        calculateRobotOffsetPoint(
                m_targetOffsetPoint,
                params.cameraCenterPoint,
                params.offsetEquationValues,
                params.robotOffsetPointMode);

        // order of this stuff doesnt matter though
        calculatePitch(params.verticalFocalLength);
        calculateYaw(params.horizontalFocalLength);
        calculateArea(params.imageArea);
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

    public Pose2d getRobotRelativePose() {
        return m_robotRelativePose;
    }

    public void setRobotRelativePose(Pose2d robotRelativePose) {
        this.m_robotRelativePose = robotRelativePose;
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

    public static class TargetCalculationParameters {
        // TargetOffset calculation values
        final boolean isLandscape;
        final TargetOffsetPointEdge targetOffsetPointEdge;

        // RobotOffset calculation values
        final Point userOffsetPoint;
        final Point cameraCenterPoint;
        final DoubleCouple offsetEquationValues;
        final RobotOffsetPointMode robotOffsetPointMode;

        // yaw calculation values
        final double horizontalFocalLength;

        // pitch calculation values
        final double verticalFocalLength;

        // area calculation values
        final double imageArea;

        public TargetCalculationParameters(
                boolean isLandscape,
                TargetOffsetPointEdge targetOffsetPointEdge,
                Point userOffsetPoint,
                Point cameraCenterPoint,
                DoubleCouple offsetEquationValues,
                RobotOffsetPointMode robotOffsetPointMode,
                double horizontalFocalLength,
                double verticalFocalLength,
                double imageArea) {
            this.isLandscape = isLandscape;
            this.targetOffsetPointEdge = targetOffsetPointEdge;
            this.userOffsetPoint = userOffsetPoint;
            this.cameraCenterPoint = cameraCenterPoint;
            this.offsetEquationValues = offsetEquationValues;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.horizontalFocalLength = horizontalFocalLength;
            this.verticalFocalLength = verticalFocalLength;
            this.imageArea = imageArea;
        }
    }
}

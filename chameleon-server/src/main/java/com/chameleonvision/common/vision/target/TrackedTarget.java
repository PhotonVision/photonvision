package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.opencv.Contour;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

// TODO: banks fix
public class TrackedTarget {
    final Contour mainContour;
    List<Contour> subContours; // can be empty

    private Point targetOffsetPoint;
    private Point robotOffsetPoint;

    private double pitch;
    private double yaw;
    private double area;

    public TrackedTarget(PotentialTarget origTarget, TargetCalculationParameters params) {
        this.mainContour = origTarget.mainContour;
        this.subContours = origTarget.subContours;
        calculateValues(params);
    }

    public Point getTargetOffsetPoint() {
        return targetOffsetPoint;
    }

    public Point getRobotOffsetPoint() {
        return robotOffsetPoint;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public double getArea() {
        return area;
    }

    public RotatedRect getMinAreaRect() {
        return mainContour.getMinAreaRect();
    }

    private void calculateTargetOffsetPoint(
            boolean isLandscape, TargetOffsetPointRegion offsetRegion) {
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
        targetOffsetPoint = resultPoint;
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

        robotOffsetPoint = resultPoint;
    }

    private void calculatePitch(double verticalFocalLength) {
        double contourCenterY = mainContour.getCenterPoint().y;
        double targetCenterY = targetOffsetPoint.y;
        pitch =
                -FastMath.toDegrees(FastMath.atan((contourCenterY - targetCenterY) / verticalFocalLength));
    }

    private void calculateYaw(double horizontalFocalLength) {
        double contourCenterX = mainContour.getCenterPoint().x;
        double targetCenterX = targetOffsetPoint.x;
        yaw =
                FastMath.toDegrees(FastMath.atan((contourCenterX - targetCenterX) / horizontalFocalLength));
    }

    private void calculateArea(double imageArea) {
        area = mainContour.getMinAreaRect().size.area() / imageArea;
    }

    private Point getMiddle(Point p1, Point p2) {
        return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
    }

    public void calculateValues(TargetCalculationParameters params) {
        // this MUST happen in this exact order!
        calculateTargetOffsetPoint(params.isLandscape, params.targetOffsetPointRegion);
        calculateRobotOffsetPoint(
                targetOffsetPoint,
                params.cameraCenterPoint,
                params.offsetEquationValues,
                params.robotOffsetPointMode);

        // order of this stuff doesnt matter though
        calculatePitch(params.verticalFocalLength);
        calculateYaw(params.horizontalFocalLength);
        calculateArea(params.imageArea);
    }

    public static class TargetCalculationParameters {
        // TargetOffset calculation values
        final boolean isLandscape;
        final TargetOffsetPointRegion targetOffsetPointRegion;

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
                TargetOffsetPointRegion targetOffsetPointRegion,
                Point userOffsetPoint,
                Point cameraCenterPoint,
                DoubleCouple offsetEquationValues,
                RobotOffsetPointMode robotOffsetPointMode,
                double horizontalFocalLength,
                double verticalFocalLength,
                double imageArea) {
            this.isLandscape = isLandscape;
            this.targetOffsetPointRegion = targetOffsetPointRegion;
            this.userOffsetPoint = userOffsetPoint;
            this.cameraCenterPoint = cameraCenterPoint;
            this.offsetEquationValues = offsetEquationValues;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.horizontalFocalLength = horizontalFocalLength;
            this.verticalFocalLength = verticalFocalLength;
            this.imageArea = imageArea;
        }
    }

    // TODO: move these? also docs plox
    public enum TargetOrientation {
        Portrait,
        Landscape
    }

    public enum TargetOffsetPointRegion {
        Center,
        Top,
        Bottom,
        Left,
        Right
    }

    public enum RobotOffsetPointMode {
        None,
        Single,
        Dual
    }
}

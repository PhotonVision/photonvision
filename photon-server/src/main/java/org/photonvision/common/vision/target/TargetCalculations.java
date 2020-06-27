package org.photonvision.common.vision.target;

import org.photonvision.common.util.numbers.DoubleCouple;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

public class TargetCalculations {
    public static double calculateYaw(
            double offsetCenterX, double targetCenterX, double horizontalFocalLength) {
        return FastMath.toDegrees(FastMath.atan(offsetCenterX - targetCenterX) / horizontalFocalLength);
    }

    public static double calculatePitch(
            double offsetCenterY, double targetCenterY, double verticalFocalLength) {
        return -FastMath.toDegrees(FastMath.atan(offsetCenterY - targetCenterY) / verticalFocalLength);
    }

    public static Point calculateTargetOffsetPoint(
            boolean isLandscape, TargetOffsetPointEdge offsetRegion, RotatedRect minAreaRect) {
        Point[] vertices = new Point[4];

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

        switch (offsetRegion) {
            case Top:
                return orientation ? tl : tr;
            case Bottom:
                return orientation ? br : bl;
            case Left:
                return orientation ? bl : tl;
            case Right:
                return orientation ? tr : br;
            default:
                return minAreaRect.center;
        }
    }

    private static Point getMiddle(Point p1, Point p2) {
        return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
    }

    public static Point calculateRobotOffsetPoint(
            Point offsetPoint,
            Point camCenterPoint,
            DoubleCouple offsetEquationValues,
            RobotOffsetPointMode offsetMode) {
        switch (offsetMode) {
            case None:
            default:
                return camCenterPoint;
            case Single:
                if (offsetPoint.x == 0 && offsetPoint.y == 0) {
                    return camCenterPoint;
                } else {
                    return offsetPoint;
                }
            case Dual:
                Point resultPoint = new Point();
                resultPoint.x =
                        (offsetPoint.x - offsetEquationValues.getFirst()) / offsetEquationValues.getSecond();
                resultPoint.y =
                        (offsetPoint.y * offsetEquationValues.getSecond()) + offsetEquationValues.getFirst();
                return resultPoint;
        }
    }
}

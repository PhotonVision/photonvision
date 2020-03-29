package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.opencv.Contour;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

// TODO: banks fix
public class TrackedTarget {
    final Contour mainContour;
    final List<Contour> subContours = new ArrayList<>(); // can be empty

    private Point targetOffsetPoint = null;
    private Point robotOffsetPoint = null;

    // Single Grouped
    public TrackedTarget(Contour inputContour) {
        mainContour = inputContour;
    }

    // Dual grouping
    public TrackedTarget(
            List<Contour> subContours,
            TargetContourIntersection intersection,
            TargetContourGrouping grouping) {
        // do contour grouping
        mainContour = calculateMultiContour(subContours, intersection, grouping);
        if (mainContour == null) {
            // this means we don't have a valid grouped target. what do we do???
            throw new RuntimeException("Something went fucky wucky");
        }
        this.subContours.addAll(subContours);
    }

    private Contour calculateMultiContour(
            List<Contour> input, TargetContourIntersection intersection, TargetContourGrouping grouping) {
        int reqSize = grouping == TargetContourGrouping.Single ? 1 : 2;

        if (input.size() != reqSize) {
            throw new RuntimeException("Insufficient contours for target grouping!");
        }

        switch (grouping) {
                // technically should never happen but :shrug:
            case Single:
                return input.get(0);
            case Dual:
                input.sort(Contour.SortByMomentsX);
                Collections.reverse(input); // why?

                Contour firstContour = input.get(0);
                Contour secondContour = input.get(1);

                // total contour for both. add the first one for now
                List<Point> fullContourPoints = new ArrayList<>(firstContour.mat.toList());

                // add second contour if it is intersecting
                if (firstContour.isIntersecting(secondContour, intersection)) {
                    fullContourPoints.addAll(secondContour.mat.toList());
                } else {
                    return null;
                }

                MatOfPoint finalContour = new MatOfPoint(fullContourPoints.toArray(new Point[0]));

                if (finalContour.cols() != 0 && finalContour.rows() != 0) {
                    return new Contour(finalContour);
                }
                break;
        }
        return null; //
    }

    private Point calculateOffsetPoint(boolean isLandscape, TargetOffsetPointRegion offsetRegion) {
        Point[] vertices = new Point[4];

        RotatedRect minRect = mainContour.getMinAreaRect();
        minRect.points(vertices);

        Point bl = getMiddle(vertices[0], vertices[1]);
        Point tl = getMiddle(vertices[1], vertices[2]);
        Point tr = getMiddle(vertices[2], vertices[3]);
        Point br = getMiddle(vertices[3], vertices[0]);
        boolean orientation;
        if (isLandscape) {
            orientation = minRect.size.width > minRect.size.height;
        } else {
            orientation = minRect.size.width < minRect.size.height;
        }

        Point result = minRect.center;
        switch (offsetRegion) {
            case Top:
                {
                    result = orientation ? tl : tr;
                    break;
                }
            case Bottom:
                {
                    result = orientation ? br : bl;
                    break;
                }
            case Left:
                {
                    result = orientation ? bl : tl;
                    break;
                }
            case Right:
                {
                    result = orientation ? tr : br;
                    break;
                }
        }
        return result;
    }

    public Point getTargetOffsetPoint(boolean isLandscape, TargetOffsetPointRegion offsetRegion) {
        if (targetOffsetPoint == null) {
            targetOffsetPoint = calculateOffsetPoint(isLandscape, offsetRegion);
        }
        return targetOffsetPoint;
    }

    private Point calculateRobotOffsetPoint(
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
        return resultPoint;
    }

    public Point getRobotOffsetPoint(
            Point userOffsetPoint,
            Point camCenterPoint,
            DoubleCouple offsetEquationValues,
            RobotOffsetPointMode offsetMode) {
        if (robotOffsetPoint == null) {
            robotOffsetPoint =
                    calculateRobotOffsetPoint(
                            userOffsetPoint, camCenterPoint, offsetEquationValues, offsetMode);
        }
        return robotOffsetPoint;
    }

    private double calculatePitch(double pixelY, double centerY, double verticalFocalLength) {
        double pitch = FastMath.toDegrees(FastMath.atan((pixelY - centerY) / verticalFocalLength));
        return (pitch * -1);
    }

    private double calculateYaw(double pixelX, double centerX, double horizontalFocalLength) {
        return FastMath.toDegrees(FastMath.atan((pixelX - centerX) / horizontalFocalLength));
    }

    private Point getMiddle(Point p1, Point p2) {
        return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
    }

    // TODO: move these? also docs plox
    public enum TargetContourIntersection {
        None,
        Up,
        Down,
        Left,
        Right
    }

    public enum TargetContourGrouping {
        Single,
        Dual
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

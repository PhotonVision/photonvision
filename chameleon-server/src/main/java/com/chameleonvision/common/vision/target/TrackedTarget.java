package com.chameleonvision.common.vision.target;

// TODO: banks fix
public class TrackedTarget {
    //    final Contour externalContour;
    //
    //    private Point targetOffsetPoint = null;
    //    private Point robotOffsetPoint = null;
    //
    //    // Single Grouped
    //    public TrackedTarget(Contour inputContour) {
    //        this.externalContour = inputContour;
    //
    //    }
    //
    //    public TrackedTarget(List<Contour> subContours) {
    //
    //    }
    //
    //
    //    private Point calculateOffsetPoint(boolean isLandscape, TargetOffsetPointRegion
    // offsetRegion) {
    //        Point[] vertices = new Point[4];
    //
    //        RotatedRect minRect = externalContour.getMinAreaRect();
    //        minRect.points(vertices);
    //
    //        Point bl = getMiddle(vertices[0], vertices[1]);
    //        Point tl = getMiddle(vertices[1], vertices[2]);
    //        Point tr = getMiddle(vertices[2], vertices[3]);
    //        Point br = getMiddle(vertices[3], vertices[0]);
    //        boolean orientation;
    //        if (isLandscape) {
    //            orientation = minRect.size.width > minRect.size.height;
    //        } else {
    //            orientation = minRect.size.width < minRect.size.height;
    //        }
    //
    //        Point result = minRect.center;
    //        switch (offsetRegion) {
    //            case Top: {
    //                result = orientation ? tl : tr;
    //                break;
    //            }
    //            case Bottom: {
    //                result = orientation ? br : bl;
    //                break;
    //            }
    //            case Left: {
    //                result = orientation ? bl : tl;
    //                break;
    //            }
    //            case Right: {
    //                result = orientation ? tr : br;
    //                break;
    //            }
    //        }
    //        return result;
    //    }
    //
    //    public Point getTargetOffsetPoint(boolean isLandscape, TargetOffsetPointRegion offsetRegion)
    // {
    //        if (targetOffsetPoint == null) {
    //            targetOffsetPoint = calculateOffsetPoint(isLandscape, offsetRegion);
    //        }
    //        return targetOffsetPoint;
    //    }
    //
    //    private Point calculateRobotOffsetPoint(DoubleCouple offsetPoint, DoubleCouple
    // offsetEquationValues, RobotOffsetPointMode offsetMode) {
    //        switch (offsetMode) {
    //            case Single:
    //                if (offsetPoint.isEmpty()) {
    //                    offsetPoint.set(camProps.centerX, camProps.centerY);
    //                }
    //
    //                t.calibratedX = offsetPoint.getFirst();
    //                t.calibratedY = offsetPoint.getSecond();
    //                break;
    //            case None:
    //                t.calibratedX = camProps.centerX;
    //                t.calibratedY = camProps.centerY;
    //                break;
    //            case Dual:
    //                t.calibratedX = (t.point.x - this.calibrationB) / this.calibrationM;
    //                t.calibratedY = (t.point.y * this.calibrationM) + this.calibrationB;
    //                break;
    //        }
    //    }
    //
    //    private double calculatePitch(double pixelY, double centerY, double verticalFocalLength) {
    //        double pitch = FastMath.toDegrees(FastMath.atan((pixelY - centerY) /
    // verticalFocalLength));
    //        return (pitch * -1);
    //    }
    //
    //    private double calculateYaw(double pixelX, double centerX, double horizontalFocalLength) {
    //        return FastMath.toDegrees(FastMath.atan((pixelX - centerX) / horizontalFocalLength));
    //    }
    //
    //    private Point getMiddle(Point p1, Point p2) {
    //        return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
    //    }
    //
    //    public enum TargetOffsetPointRegion {
    //        Center, Top, Bottom, Left, Right
    //    }
    //
    //    public enum RobotOffsetPointMode {
    //        None, Single, Dual
    //    }
}

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

import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.opencv.DualOffsetValues;

public class TargetCalculations {
    public static double calculateYaw(
            double offsetCenterX, double targetCenterX, double horizontalFocalLength) {
        return Math.toDegrees(Math.atan((offsetCenterX - targetCenterX) / horizontalFocalLength));
    }

    public static double calculatePitch(
            double offsetCenterY, double targetCenterY, double verticalFocalLength) {
        return -Math.toDegrees(Math.atan((offsetCenterY - targetCenterY) / verticalFocalLength));
    }

    @SuppressWarnings("DuplicatedCode")
    public static double calculateSkew(boolean isLandscape, RotatedRect minAreaRect) {
        // https://namkeenman.wordpress.com/2015/12/18/open-cv-determine-angle-of-rotatedrect-minarearect/
        var angle = minAreaRect.angle;

        if (isLandscape && minAreaRect.size.width < minAreaRect.size.height) angle += 90;
        else if (!isLandscape && minAreaRect.size.height < minAreaRect.size.width) angle += 90;

        // Ensure skew is bounded on [-90, 90]
        while (angle > 90) angle -= 180;
        while (angle < -90) angle += 180;

        return angle;
    }

    public static Point calculateTargetOffsetPoint(
            boolean isLandscape, TargetOffsetPointEdge offsetRegion, RotatedRect minAreaRect) {
        Point[] vertices = new Point[4];

        minAreaRect.points(vertices);

        Point bottom = getMiddle(vertices[0], vertices[1]);
        Point left = getMiddle(vertices[1], vertices[2]);
        Point top = getMiddle(vertices[2], vertices[3]);
        Point right = getMiddle(vertices[3], vertices[0]);

        boolean orientationCorrect = minAreaRect.size.width > minAreaRect.size.height;
        if (!isLandscape) orientationCorrect = !orientationCorrect;

        switch (offsetRegion) {
            case Top:
                if (orientationCorrect) return (left.y < right.y) ? left : right;
                else return (top.y < bottom.y) ? top : bottom;
            case Bottom:
                if (orientationCorrect) return (left.y > right.y) ? left : right;
                else return (top.y > bottom.y) ? top : bottom;
            case Left:
                if (orientationCorrect) return (top.x < bottom.x) ? top : bottom;
                else return (left.x < right.x) ? left : right;
            case Right:
                if (orientationCorrect) return (top.x > bottom.x) ? top : bottom;
                else return (left.x > right.x) ? left : right;
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
            DualOffsetValues dualOffsetValues,
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
                var resultPoint = new Point();
                var lineValues = dualOffsetValues.getLineValues();
                var offsetSlope = lineValues.getFirst();
                var offsetIntercept = lineValues.getSecond();

                resultPoint.x = (offsetPoint.x - offsetIntercept) / offsetSlope;
                resultPoint.y = (offsetPoint.y * offsetSlope) + offsetIntercept;
                return resultPoint;
        }
    }

    public static double getAspectRatio(RotatedRect rect, boolean isLandscape) {
        if (rect.size.width == 0 || rect.size.height == 0) return 0;
        double ratio = rect.size.width / rect.size.height;

        // In landscape, we should be shorter than we are wide (that is, aspect ratio should be >1)
        if (isLandscape && ratio < 1) {
            ratio = 1.0 / ratio;
        }

        // If portrait, should always be taller than wide (ratio < 1)
        else if (!isLandscape && ratio > 1) {
            ratio = 1.0 / ratio;
        }

        return ratio;
    }

    public static Point calculateDualOffsetCrosshair(
            DualOffsetValues dualOffsetValues, double currentArea) {
        boolean firstLarger = dualOffsetValues.firstPointArea >= dualOffsetValues.secondPointArea;
        double upperArea =
                firstLarger ? dualOffsetValues.secondPointArea : dualOffsetValues.firstPointArea;
        double lowerArea =
                firstLarger ? dualOffsetValues.firstPointArea : dualOffsetValues.secondPointArea;

        var areaFraction = MathUtils.map(currentArea, lowerArea, upperArea, 0, 1);
        var xLerp =
                MathUtils.lerp(dualOffsetValues.firstPoint.x, dualOffsetValues.secondPoint.x, areaFraction);
        var yLerp =
                MathUtils.lerp(dualOffsetValues.firstPoint.y, dualOffsetValues.secondPoint.y, areaFraction);

        return new Point(xLerp, yLerp);
    }

    public static DoubleCouple getLineFromPoints(Point firstPoint, Point secondPoint) {
        var offsetLineSlope = (secondPoint.y - firstPoint.y) / (secondPoint.x - firstPoint.x);
        var offsetLineIntercept = firstPoint.y - (offsetLineSlope * firstPoint.x);
        return new DoubleCouple(offsetLineSlope, offsetLineIntercept);
    }
}

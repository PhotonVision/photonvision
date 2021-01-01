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

import edu.wpi.first.wpilibj.trajectory.Trajectory;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.opencv.DualOffsetValues;

public class TargetCalculations {
    public static double calculateYaw(
            double offsetCenterX, double targetCenterX, double horizontalFocalLength) {
        return FastMath.toDegrees(
                FastMath.atan((offsetCenterX - targetCenterX) / horizontalFocalLength));
    }

    public static double calculatePitch(
            double offsetCenterY, double targetCenterY, double verticalFocalLength) {
        return -FastMath.toDegrees(
                FastMath.atan((offsetCenterY - targetCenterY) / verticalFocalLength));
    }

    @SuppressWarnings("DuplicatedCode")
    public static double calculateSkew(boolean isLandscape, RotatedRect minAreaRect) {
        // https://namkeenman.wordpress.com/2015/12/18/open-cv-determine-angle-of-rotatedrect-minarearect/
        var angle = minAreaRect.angle;

        if (isLandscape && minAreaRect.size.width < minAreaRect.size.height) angle += 90;
        else if (!isLandscape && minAreaRect.size.height < minAreaRect.size.width) angle += 90;

        return angle;
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

    public static Point calculateDualOffsetCrosshair(
            DualOffsetValues dualOffsetValues, double currentArea) {
        boolean firstLarger = dualOffsetValues.firstPointArea >= dualOffsetValues.secondPointArea;
        double upperArea =
                firstLarger ? dualOffsetValues.secondPointArea : dualOffsetValues.firstPointArea;
        double lowerArea =
                firstLarger ? dualOffsetValues.firstPointArea : dualOffsetValues.secondPointArea;

        var areaFraction = MathUtils.map(currentArea, lowerArea, upperArea, 0, 1);
        var xLerp =
                Trajectory.State.lerp(
                        dualOffsetValues.firstPoint.x, dualOffsetValues.secondPoint.x, areaFraction);
        var yLerp =
                Trajectory.State.lerp(
                        dualOffsetValues.firstPoint.y, dualOffsetValues.secondPoint.y, areaFraction);

        return new Point(xLerp, yLerp);
    }

    public static DoubleCouple getLineFromPoints(Point firstPoint, Point secondPoint) {
        var offsetLineSlope = (secondPoint.y - firstPoint.y) / (secondPoint.x - firstPoint.x);
        var offsetLineIntercept = firstPoint.y - (offsetLineSlope * firstPoint.x);
        return new DoubleCouple(offsetLineSlope, offsetLineIntercept);
    }
}

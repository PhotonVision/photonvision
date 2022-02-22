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

package org.photonvision.vision.pipeline;

import java.util.Objects;
import org.opencv.core.Point;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.opencv.ContourSortMode;
import org.photonvision.vision.pipe.impl.CornerDetectionPipe;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TargetOffsetPointEdge;
import org.photonvision.vision.target.TargetOrientation;

public class AdvancedPipelineSettings extends CVPipelineSettings {
    public AdvancedPipelineSettings() {
        ledMode = true;
    }

    public IntegerCouple hsvHue = new IntegerCouple(50, 180);
    public IntegerCouple hsvSaturation = new IntegerCouple(50, 255);
    public IntegerCouple hsvValue = new IntegerCouple(50, 255);
    public boolean hueInverted = false;

    public boolean outputShouldDraw = true;
    public boolean outputShowMultipleTargets = false;

    public DoubleCouple contourArea = new DoubleCouple(0.0, 100.0);
    public DoubleCouple contourRatio = new DoubleCouple(0.0, 20.0);
    public DoubleCouple contourFullness = new DoubleCouple(0.0, 100.0);
    public int contourSpecklePercentage = 5;

    // the order in which to sort contours to find the most desirable
    public ContourSortMode contourSortMode = ContourSortMode.Largest;

    // the edge (or not) of the target to consider the center point (Top, Bottom, Left, Right,
    // Center)
    public TargetOffsetPointEdge contourTargetOffsetPointEdge = TargetOffsetPointEdge.Center;

    // orientation of the target in terms of aspect ratio
    public TargetOrientation contourTargetOrientation = TargetOrientation.Landscape;

    // the mode in which to offset target center point based on the camera being offset on the
    // robot
    // (None, Single Point, Dual Point)
    public RobotOffsetPointMode offsetRobotOffsetMode = RobotOffsetPointMode.None;

    // the point set by the user in Single Point Offset mode (maybe double too? idr)
    public Point offsetSinglePoint = new Point();

    // the two values that define the line of the Dual Point Offset calibration (think y=mx+b)
    public Point offsetDualPointA = new Point();
    public double offsetDualPointAArea = 0;
    public Point offsetDualPointB = new Point();
    public double offsetDualPointBArea = 0;

    // how many contours to attempt to group (Single, Dual)
    public ContourGroupingMode contourGroupingMode = ContourGroupingMode.Single;

    // the direction in which contours must intersect to be considered intersecting
    public ContourIntersectionDirection contourIntersection = ContourIntersectionDirection.Up;

    // 3d settings
    public boolean solvePNPEnabled = false;
    public TargetModel targetModel = TargetModel.k2020HighGoalOuter;

    // Corner detection settings
    public CornerDetectionPipe.DetectionStrategy cornerDetectionStrategy =
            CornerDetectionPipe.DetectionStrategy.APPROX_POLY_DP_AND_EXTREME_CORNERS;
    public boolean cornerDetectionUseConvexHulls = true;
    public boolean cornerDetectionExactSideCount = false;
    public int cornerDetectionSideCount = 4;
    public double cornerDetectionAccuracyPercentage = 10;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvancedPipelineSettings)) return false;
        if (!super.equals(o)) return false;
        AdvancedPipelineSettings that = (AdvancedPipelineSettings) o;
        return outputShouldDraw == that.outputShouldDraw
                && outputShowMultipleTargets == that.outputShowMultipleTargets
                && contourSpecklePercentage == that.contourSpecklePercentage
                && Double.compare(that.offsetDualPointAArea, offsetDualPointAArea) == 0
                && Double.compare(that.offsetDualPointBArea, offsetDualPointBArea) == 0
                && solvePNPEnabled == that.solvePNPEnabled
                && cornerDetectionUseConvexHulls == that.cornerDetectionUseConvexHulls
                && cornerDetectionExactSideCount == that.cornerDetectionExactSideCount
                && cornerDetectionSideCount == that.cornerDetectionSideCount
                && Double.compare(that.cornerDetectionAccuracyPercentage, cornerDetectionAccuracyPercentage)
                        == 0
                && Objects.equals(hsvHue, that.hsvHue)
                && Objects.equals(hsvSaturation, that.hsvSaturation)
                && Objects.equals(hsvValue, that.hsvValue)
                && Objects.equals(hueInverted, that.hueInverted)
                && Objects.equals(contourArea, that.contourArea)
                && Objects.equals(contourRatio, that.contourRatio)
                && Objects.equals(contourFullness, that.contourFullness)
                && contourSortMode == that.contourSortMode
                && contourTargetOffsetPointEdge == that.contourTargetOffsetPointEdge
                && contourTargetOrientation == that.contourTargetOrientation
                && offsetRobotOffsetMode == that.offsetRobotOffsetMode
                && Objects.equals(offsetSinglePoint, that.offsetSinglePoint)
                && Objects.equals(offsetDualPointA, that.offsetDualPointA)
                && Objects.equals(offsetDualPointB, that.offsetDualPointB)
                && contourGroupingMode == that.contourGroupingMode
                && contourIntersection == that.contourIntersection
                && Objects.equals(targetModel, that.targetModel)
                && cornerDetectionStrategy == that.cornerDetectionStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                hsvHue,
                hsvSaturation,
                hsvValue,
                hueInverted,
                outputShouldDraw,
                outputShowMultipleTargets,
                contourArea,
                contourRatio,
                contourFullness,
                contourSpecklePercentage,
                contourSortMode,
                contourTargetOffsetPointEdge,
                contourTargetOrientation,
                offsetRobotOffsetMode,
                offsetSinglePoint,
                offsetDualPointA,
                offsetDualPointAArea,
                offsetDualPointB,
                offsetDualPointBArea,
                contourGroupingMode,
                contourIntersection,
                solvePNPEnabled,
                targetModel,
                cornerDetectionStrategy,
                cornerDetectionUseConvexHulls,
                cornerDetectionExactSideCount,
                cornerDetectionSideCount,
                cornerDetectionAccuracyPercentage);
    }
}

/*
 * Copyright (C) 2020 Photon Vision.
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
import org.photonvision.vision.opencv.ContourSortMode;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TargetOffsetPointEdge;
import org.photonvision.vision.target.TargetOrientation;

public class AdvancedPipelineSettings extends CVPipelineSettings {

    public AdvancedPipelineSettings() {
        ledMode = true;
    }

    public IntegerCouple hsvHue = new IntegerCouple(50, 180);
    public IntegerCouple hsvSaturation = new IntegerCouple(50, 255);
    public IntegerCouple hsvValue = new IntegerCouple(50, 255);

    public boolean outputShouldDraw = true;
    public boolean outputShowMultipleTargets = false;

    public boolean erode = false;
    public boolean dilate = false;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AdvancedPipelineSettings that = (AdvancedPipelineSettings) o;
        return outputShouldDraw == that.outputShouldDraw
                && outputShowMultipleTargets == that.outputShowMultipleTargets
                && erode == that.erode
                && dilate == that.dilate
                && contourSpecklePercentage == that.contourSpecklePercentage
                && Double.compare(that.offsetDualPointA.x, offsetDualPointA.x) == 0
                && Double.compare(that.offsetDualPointA.y, offsetDualPointA.y) == 0
                && Double.compare(that.offsetDualPointAArea, offsetDualPointAArea) == 0
                && Double.compare(that.offsetDualPointB.x, offsetDualPointB.x) == 0
                && Double.compare(that.offsetDualPointB.y, offsetDualPointB.y) == 0
                && Double.compare(that.offsetDualPointBArea, offsetDualPointBArea) == 0
                && hsvHue.equals(that.hsvHue)
                && hsvSaturation.equals(that.hsvSaturation)
                && hsvValue.equals(that.hsvValue)
                && contourArea.equals(that.contourArea)
                && contourRatio.equals(that.contourRatio)
                && contourFullness.equals(that.contourFullness)
                && contourSortMode == that.contourSortMode
                && contourTargetOffsetPointEdge == that.contourTargetOffsetPointEdge
                && contourTargetOrientation == that.contourTargetOrientation
                && offsetRobotOffsetMode == that.offsetRobotOffsetMode
                && offsetSinglePoint.equals(that.offsetSinglePoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                hsvHue,
                hsvSaturation,
                hsvValue,
                outputShouldDraw,
                outputShowMultipleTargets,
                erode,
                dilate,
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
                offsetDualPointBArea);
    }
}

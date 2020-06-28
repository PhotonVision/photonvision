package org.photonvision.vision.pipeline;

import java.util.Objects;
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

    public boolean outputShowThresholded = false;
    public boolean outputShowMultipleTargets = false;

    public boolean erode = false;
    public boolean dilate = false;

    public DoubleCouple contourArea = new DoubleCouple(0.0, 100.0);
    public DoubleCouple contourRatio = new DoubleCouple(0.0, 20.0);
    public DoubleCouple contourExtent = new DoubleCouple(0.0, 100.0);
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
    public DoubleCouple offsetCalibrationPoint = new DoubleCouple();

    // the two values that define the line of the Dual Point Offset calibration (think y=mx+b)
    public double offsetDualLineM = 1;
    public double offsetDualLineB = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AdvancedPipelineSettings that = (AdvancedPipelineSettings) o;
        return outputShowThresholded == that.outputShowThresholded
                && outputShowMultipleTargets == that.outputShowMultipleTargets
                && erode == that.erode
                && dilate == that.dilate
                && contourSpecklePercentage == that.contourSpecklePercentage
                && Double.compare(that.offsetDualLineM, offsetDualLineM) == 0
                && Double.compare(that.offsetDualLineB, offsetDualLineB) == 0
                && hsvHue.equals(that.hsvHue)
                && hsvSaturation.equals(that.hsvSaturation)
                && hsvValue.equals(that.hsvValue)
                && contourArea.equals(that.contourArea)
                && contourRatio.equals(that.contourRatio)
                && contourExtent.equals(that.contourExtent)
                && contourSortMode == that.contourSortMode
                && contourTargetOffsetPointEdge == that.contourTargetOffsetPointEdge
                && contourTargetOrientation == that.contourTargetOrientation
                && offsetRobotOffsetMode == that.offsetRobotOffsetMode
                && offsetCalibrationPoint.equals(that.offsetCalibrationPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                hsvHue,
                hsvSaturation,
                hsvValue,
                outputShowThresholded,
                outputShowMultipleTargets,
                erode,
                dilate,
                contourArea,
                contourRatio,
                contourExtent,
                contourSpecklePercentage,
                contourSortMode,
                contourTargetOffsetPointEdge,
                contourTargetOrientation,
                offsetRobotOffsetMode,
                offsetCalibrationPoint,
                offsetDualLineM,
                offsetDualLineB);
    }
}

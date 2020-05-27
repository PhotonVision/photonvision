package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.util.numbers.IntegerCouple;
import com.chameleonvision.common.vision.opencv.ContourSortMode;
import com.chameleonvision.common.vision.target.RobotOffsetPointMode;
import com.chameleonvision.common.vision.target.TargetOffsetPointEdge;
import com.chameleonvision.common.vision.target.TargetOrientation;

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
}

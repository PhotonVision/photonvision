package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.util.numbers.IntegerCouple;
import com.chameleonvision.common.vision.opencv.ContourGroupingMode;
import com.chameleonvision.common.vision.opencv.ContourIntersectionDirection;
import com.chameleonvision.common.vision.opencv.ContourSortMode;
import com.chameleonvision.common.vision.pipe.impl.CornerDetectionPipe;
import com.chameleonvision.common.vision.target.RobotOffsetPointMode;
import com.chameleonvision.common.vision.target.TargetModel;
import com.chameleonvision.common.vision.target.TargetOffsetPointEdge;
import com.chameleonvision.common.vision.target.TargetOrientation;
import edu.wpi.first.wpilibj.geometry.Rotation2d;

public class ReflectivePipelineSettings extends CVPipelineSettings {
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

    // how many contours to attempt to group (Single, Dual)
    public ContourGroupingMode contourGroupingMode = ContourGroupingMode.Single;

    // the direction in which contours must intersect to be considered intersecting
    public ContourIntersectionDirection contourIntersection = ContourIntersectionDirection.Up;

    // the mode in which to offset target center point based on the camera being offset on the
    // robot
    // (None, Single Point, Dual Point)
    public RobotOffsetPointMode offsetRobotOffsetMode = RobotOffsetPointMode.None;

    // the point set by the user in Single Point Offset mode (maybe double too? idr)
    public DoubleCouple offsetCalibrationPoint = new DoubleCouple();

    // the two values that define the line of the Dual Point Offset calibration (think y=mx+b)
    public double offsetDualLineM = 1;
    public double offsetDualLineB = 0;

    // 3d settings
    public boolean solvePNPEnabled = false;
    public CameraCalibrationCoefficients cameraCalibration;
    public TargetModel targetModel;
    public Rotation2d cameraPitch;

    // Corner detection settings
    public CornerDetectionPipe.DetectionStrategy cornerDetectionStrategy =
            CornerDetectionPipe.DetectionStrategy.APPROX_POLY_DP_AND_EXTREME_CORNERS;
    public boolean cornerDetectionUseConvexHulls = true;
    public boolean cornerDetectionExactSideCount = false;
    public int cornerDetectionSideCount = 4;
    public double cornerDetectionAccuracyPercentage = 10;
}

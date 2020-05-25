package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.vision.opencv.ContourGroupingMode;
import com.chameleonvision.common.vision.opencv.ContourIntersectionDirection;
import com.chameleonvision.common.vision.pipe.impl.CornerDetectionPipe;
import com.chameleonvision.common.vision.target.TargetModel;
import edu.wpi.first.wpilibj.geometry.Rotation2d;

public class ReflectivePipelineSettings extends AdvancedPipelineSettings {
    // how many contours to attempt to group (Single, Dual)
    public ContourGroupingMode contourGroupingMode = ContourGroupingMode.Single;

    // the direction in which contours must intersect to be considered intersecting
    public ContourIntersectionDirection contourIntersection = ContourIntersectionDirection.Up;

    // 3d settings
    public boolean solvePNPEnabled = false;
    public CameraCalibrationCoefficients cameraCalibration;
    public TargetModel targetModel;
    public Rotation2d cameraPitch = Rotation2d.fromDegrees(0.0);

    // Corner detection settings
    public CornerDetectionPipe.DetectionStrategy cornerDetectionStrategy =
            CornerDetectionPipe.DetectionStrategy.APPROX_POLY_DP_AND_EXTREME_CORNERS;
    public boolean cornerDetectionUseConvexHulls = true;
    public boolean cornerDetectionExactSideCount = false;
    public int cornerDetectionSideCount = 4;
    public double cornerDetectionAccuracyPercentage = 10;
}

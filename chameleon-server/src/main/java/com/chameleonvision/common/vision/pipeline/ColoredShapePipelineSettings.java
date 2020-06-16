package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.vision.opencv.ContourGroupingMode;
import com.chameleonvision.common.vision.opencv.ContourIntersectionDirection;
import com.chameleonvision.common.vision.opencv.ContourShape;
import com.chameleonvision.common.vision.pipe.impl.CornerDetectionPipe;
import com.chameleonvision.common.vision.target.TargetModel;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.util.Objects;

@JsonTypeName("ColoredShapePipelineSettings")
public class ColoredShapePipelineSettings extends AdvancedPipelineSettings {
    public ContourShape desiredShape = ContourShape.Triangle;
    public double minArea = Integer.MIN_VALUE;
    public double maxArea = Integer.MAX_VALUE;
    public double minPeri = Integer.MIN_VALUE;
    public double maxPeri = Integer.MAX_VALUE;
    public double accuracyPercentage = 10.0;
    // Circle detection
    public int allowableThreshold = 5;
    public int minRadius = 0;
    public int maxRadius = 0;
    public int minDist = 10;
    public int maxCannyThresh = 90;
    public int accuracy = 20;
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

    public ColoredShapePipelineSettings() {
        super();
        pipelineType = PipelineType.ColoredShape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColoredShapePipelineSettings that = (ColoredShapePipelineSettings) o;
        return Double.compare(that.minArea, minArea) == 0
                && Double.compare(that.maxArea, maxArea) == 0
                && Double.compare(that.minPeri, minPeri) == 0
                && Double.compare(that.maxPeri, maxPeri) == 0
                && Double.compare(that.accuracyPercentage, accuracyPercentage) == 0
                && allowableThreshold == that.allowableThreshold
                && minRadius == that.minRadius
                && maxRadius == that.maxRadius
                && minDist == that.minDist
                && maxCannyThresh == that.maxCannyThresh
                && accuracy == that.accuracy
                && solvePNPEnabled == that.solvePNPEnabled
                && cornerDetectionUseConvexHulls == that.cornerDetectionUseConvexHulls
                && cornerDetectionExactSideCount == that.cornerDetectionExactSideCount
                && cornerDetectionSideCount == that.cornerDetectionSideCount
                && Double.compare(that.cornerDetectionAccuracyPercentage, cornerDetectionAccuracyPercentage)
                        == 0
                && desiredShape == that.desiredShape
                && contourGroupingMode == that.contourGroupingMode
                && contourIntersection == that.contourIntersection
                && Objects.equals(cameraCalibration, that.cameraCalibration)
                && Objects.equals(targetModel, that.targetModel)
                && Objects.equals(cameraPitch, that.cameraPitch)
                && cornerDetectionStrategy == that.cornerDetectionStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                desiredShape,
                minArea,
                maxArea,
                minPeri,
                maxPeri,
                accuracyPercentage,
                allowableThreshold,
                minRadius,
                maxRadius,
                minDist,
                maxCannyThresh,
                accuracy,
                contourGroupingMode,
                contourIntersection,
                solvePNPEnabled,
                cameraCalibration,
                targetModel,
                cameraPitch,
                cornerDetectionStrategy,
                cornerDetectionUseConvexHulls,
                cornerDetectionExactSideCount,
                cornerDetectionSideCount,
                cornerDetectionAccuracyPercentage);
    }
}

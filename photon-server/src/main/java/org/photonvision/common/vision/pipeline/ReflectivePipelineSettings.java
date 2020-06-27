package org.photonvision.common.vision.pipeline;

import org.photonvision.common.calibration.CameraCalibrationCoefficients;
import org.photonvision.common.vision.opencv.ContourGroupingMode;
import org.photonvision.common.vision.opencv.ContourIntersectionDirection;
import org.photonvision.common.vision.pipe.impl.CornerDetectionPipe;
import org.photonvision.common.vision.target.TargetModel;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.wpi.first.wpilibj.geometry.Rotation2d;

import java.util.Objects;

@JsonTypeName("ReflectivePipelineSettings")
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

    public ReflectivePipelineSettings() {
        super();
        pipelineType = PipelineType.Reflective;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReflectivePipelineSettings that = (ReflectivePipelineSettings) o;
        return solvePNPEnabled == that.solvePNPEnabled
                && cornerDetectionUseConvexHulls == that.cornerDetectionUseConvexHulls
                && cornerDetectionExactSideCount == that.cornerDetectionExactSideCount
                && cornerDetectionSideCount == that.cornerDetectionSideCount
                && Double.compare(that.cornerDetectionAccuracyPercentage, cornerDetectionAccuracyPercentage)
                        == 0
                && contourGroupingMode == that.contourGroupingMode
                && contourIntersection == that.contourIntersection
                && Objects.equals(cameraCalibration, that.cameraCalibration)
                && targetModel.equals(that.targetModel)
                && cameraPitch.equals(that.cameraPitch)
                && cornerDetectionStrategy == that.cornerDetectionStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
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

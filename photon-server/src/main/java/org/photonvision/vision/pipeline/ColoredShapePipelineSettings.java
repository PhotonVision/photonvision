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

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.util.Objects;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.impl.CornerDetectionPipe;
import org.photonvision.vision.target.TargetModel;

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
    public Rotation2d cameraPitch = Rotation2d.fromDegrees(0.0); // TODO where should pitch live?

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

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

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.opencv.ContourShape;

@JsonTypeName("ColoredShapePipelineSettings")
public class ColoredShapePipelineSettings extends AdvancedPipelineSettings {
    public ContourShape contourShape = ContourShape.Triangle;
    public DoubleCouple contourPerimeter = new DoubleCouple(0, Double.MAX_VALUE);
    public double accuracyPercentage = 10.0;
    // Circle detection
    public int circleDetectThreshold = 5;
    public IntegerCouple contourRadius = new IntegerCouple(0, 100);
    public int minDist = 20;
    public int maxCannyThresh = 90;
    public int circleAccuracy = 20;

    // 3d settings
    public CameraCalibrationCoefficients cameraCalibration;

    public boolean erode = false;
    public boolean dilate = false;

    public ColoredShapePipelineSettings() {
        super();
        pipelineType = PipelineType.ColoredShape;
        cameraExposure = 20;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColoredShapePipelineSettings that = (ColoredShapePipelineSettings) o;
        return Double.compare(that.accuracyPercentage, accuracyPercentage) == 0
                && circleDetectThreshold == that.circleDetectThreshold
                && minDist == that.minDist
                && maxCannyThresh == that.maxCannyThresh
                && circleAccuracy == that.circleAccuracy
                && cornerDetectionUseConvexHulls == that.cornerDetectionUseConvexHulls
                && cornerDetectionExactSideCount == that.cornerDetectionExactSideCount
                && cornerDetectionSideCount == that.cornerDetectionSideCount
                && Double.compare(that.cornerDetectionAccuracyPercentage, cornerDetectionAccuracyPercentage)
                        == 0
                && erode == that.erode
                && dilate == that.dilate
                && contourShape == that.contourShape
                && Objects.equals(contourArea, that.contourArea)
                && Objects.equals(contourPerimeter, that.contourPerimeter)
                && Objects.equals(contourRadius, that.contourRadius)
                && contourGroupingMode == that.contourGroupingMode
                && contourIntersection == that.contourIntersection
                && Objects.equals(cameraCalibration, that.cameraCalibration)
                && cornerDetectionStrategy == that.cornerDetectionStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                contourShape,
                contourArea,
                contourPerimeter,
                accuracyPercentage,
                circleDetectThreshold,
                contourRadius,
                minDist,
                maxCannyThresh,
                circleAccuracy,
                contourGroupingMode,
                contourIntersection,
                cameraCalibration,
                cornerDetectionStrategy,
                cornerDetectionUseConvexHulls,
                cornerDetectionExactSideCount,
                cornerDetectionSideCount,
                cornerDetectionAccuracyPercentage,
                erode,
                dilate);
    }
}

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
import java.util.Objects;
import org.photonvision.vision.opencv.ContourShape;

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

    public ColoredShapePipelineSettings() {
        super();
        pipelineType = PipelineType.ColoredShape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColoredShapePipelineSettings)) return false;
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
                && desiredShape == that.desiredShape;
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
                accuracy);
    }
}

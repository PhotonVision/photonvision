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

package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.CVPipe;

public class FilterShapesPipe
        extends CVPipe<List<CVShape>, List<CVShape>, FilterShapesPipe.FilterShapesPipeParams> {
    List<CVShape> outputList = new ArrayList<>();

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing.
     * @return Result of processing.
     */
    @Override
    protected List<CVShape> process(List<CVShape> in) {
        outputList.forEach(CVShape::release);
        outputList.clear();
        outputList = new ArrayList<>();

        for (var shape : in) {
            if (!shouldRemove(shape)) outputList.add(shape);
        }

        return outputList;
    }

    private boolean shouldRemove(CVShape shape) {
        return shape.shape != params.desiredShape
                || shape.contour.getArea() / params.getFrameStaticProperties().imageArea * 100.0
                        > params.maxArea
                || shape.contour.getArea() / params.getFrameStaticProperties().imageArea * 100.0
                        < params.minArea
                || shape.contour.getPerimeter() > params.maxPeri
                || shape.contour.getPerimeter() < params.minPeri;
    }

    public static class FilterShapesPipeParams {
        private final ContourShape desiredShape;
        private final FrameStaticProperties frameStaticProperties;
        private final double minArea;
        private final double maxArea;
        private final double minPeri;
        private final double maxPeri;

        public FilterShapesPipeParams(
                ContourShape desiredShape,
                double minArea,
                double maxArea,
                double minPeri,
                double maxPeri,
                FrameStaticProperties frameStaticProperties) {
            this.desiredShape = desiredShape;
            this.minArea = minArea;
            this.maxArea = maxArea;
            this.minPeri = minPeri;
            this.maxPeri = maxPeri;
            this.frameStaticProperties = frameStaticProperties;
        }

        public FrameStaticProperties getFrameStaticProperties() {
            return frameStaticProperties;
        }
    }
}

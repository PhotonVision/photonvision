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

import java.util.List;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.CVPipe;

public class FilterShapesPipe
        extends CVPipe<List<CVShape>, List<CVShape>, FilterShapesPipe.FilterShapesPipeParams> {
    /**
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return Result of processing.
    */
    @Override
    protected List<CVShape> process(List<CVShape> in) {
        in.removeIf(
                shape ->
                        shape.shape != params.desiredShape
                                || shape.contour.getArea() > params.maxArea
                                || shape.contour.getArea() < params.minArea
                                || shape.contour.getPerimeter() > params.maxPeri
                                || shape.contour.getPerimeter() < params.minPeri);
        return in;
    }

    public static class FilterShapesPipeParams {
        private final ContourShape desiredShape;
        private final double minArea;
        private final double maxArea;
        private final double minPeri;
        private final double maxPeri;

        public FilterShapesPipeParams(
                ContourShape desiredShape, double minArea, double maxArea, double minPeri, double maxPeri) {
            this.desiredShape = desiredShape;
            this.minArea = minArea;
            this.maxArea = maxArea;
            this.minPeri = minPeri;
            this.maxPeri = maxPeri;
        }
    }
}

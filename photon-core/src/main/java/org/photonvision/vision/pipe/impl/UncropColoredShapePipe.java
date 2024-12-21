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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;

/**
 * A pipe that offsets the coordinates of CVShapes (from a ColoredShapePipeline) back into the
 * original (uncropped) image coordinate system by adjusting their contours.
 */
public class UncropColoredShapePipe extends CVPipe<List<CVShape>, List<CVShape>, Rect> {
    public UncropColoredShapePipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected List<CVShape> process(List<CVShape> in) {
        List<CVShape> uncroppedShapes = new ArrayList<>();

        double dx = this.params.x;
        double dy = this.params.y;

        for (CVShape shape : in) {
            Contour originalContour = shape.getContour();
            Contour shiftedContour = offsetContour(originalContour, dx, dy);

            CVShape adjustedShape = new CVShape(shiftedContour, shape.shape);
            uncroppedShapes.add(adjustedShape);
        }

        return uncroppedShapes;
    }

    private Contour offsetContour(Contour contour, double dx, double dy) {
        Point[] originalPoints = contour.mat.toArray();
        Point[] shiftedPoints = new Point[originalPoints.length];

        for (int i = 0; i < originalPoints.length; i++) {
            shiftedPoints[i] = new Point(originalPoints[i].x + dx, originalPoints[i].y + dy);
        }

        MatOfPoint newMat = new MatOfPoint(shiftedPoints);
        return new Contour(newMat);
    }
}

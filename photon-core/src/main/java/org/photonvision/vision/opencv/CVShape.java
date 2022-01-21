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

package org.photonvision.vision.opencv;

import org.jetbrains.annotations.Nullable;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;

public class CVShape implements Releasable {
    public final Contour contour;

    @Nullable public final ContourShape shape;

    public double radius = 0;
    public Point center = null;

    private MatOfPoint3f customTarget = null;

    private MatOfPoint2f approxCurve = new MatOfPoint2f();

    public CVShape(Contour contour, ContourShape shape) {
        this.contour = contour;
        this.shape = shape;
    }

    public CVShape(Contour contour, Point center, double radius) {
        this(contour, ContourShape.Circle);
        this.radius = radius;
        this.center = center;
    }

    public CVShape(Contour contour, MatOfPoint3f targetPoints) {
        this.contour = contour;
        this.shape = ContourShape.Custom;
        customTarget = targetPoints;
    }

    public Contour getContour() {
        return contour;
    }

    @Override
    public void release() {
        if (customTarget != null) customTarget.release();
        approxCurve.release();
        contour.release();
    }
}

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

import org.opencv.core.Point;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.target.TargetCalculations;

public class DualOffsetValues {
    public final Point firstPoint;
    public final double firstPointArea;
    public final Point secondPoint;
    public final double secondPointArea;

    public DualOffsetValues() {
        firstPoint = new Point();
        firstPointArea = 0;
        secondPoint = new Point();
        secondPointArea = 0;
    }

    public DualOffsetValues(
            Point firstPoint, double firstPointArea, Point secondPoint, double secondPointArea) {
        this.firstPoint = firstPoint;
        this.firstPointArea = firstPointArea;
        this.secondPoint = secondPoint;
        this.secondPointArea = secondPointArea;
    }

    public DoubleCouple getLineValues() {
        return TargetCalculations.getLineFromPoints(firstPoint, secondPoint);
    }
}

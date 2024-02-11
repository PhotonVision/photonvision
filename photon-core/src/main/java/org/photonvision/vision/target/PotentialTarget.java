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
package org.photonvision.vision.target;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.RotatedRect;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class PotentialTarget implements Releasable {

    public final Contour m_mainContour;
    public final List<Contour> m_subContours;
    public final CVShape shape;

    // additional metadata about object detections we need to keep around
    public final double confidence;
    public final int clsId;

    public PotentialTarget(Contour inputContour) {
        this(inputContour, List.of());
    }

    public PotentialTarget(Contour inputContour, List<Contour> subContours) {
        this(inputContour, subContours, null);
    }

    public PotentialTarget(Contour inputContour, List<Contour> subContours, CVShape shape) {
        m_mainContour = inputContour;
        m_subContours = new ArrayList<>(subContours);
        this.shape = shape;
        this.clsId = -1;
        this.confidence = -1;
    }

    public PotentialTarget(Contour inputContour, CVShape shape) {
        this(inputContour, List.of(), shape);
    }

    public PotentialTarget(NeuralNetworkPipeResult det) {
        this.shape = new CVShape(new Contour(det.box), ContourShape.Quadrilateral);
        this.m_mainContour = this.shape.getContour();
        m_subContours = List.of();
        this.clsId = det.classIdx;
        this.confidence = det.confidence;
    }

    public PotentialTarget(CVShape cvShape) {
        this(cvShape.getContour(), cvShape);
    }

    public RotatedRect getMinAreaRect() {
        return m_mainContour.getMinAreaRect();
    }

    public double getArea() {
        return m_mainContour.getArea();
    }

    @Override
    public void release() {
        m_mainContour.release();
        for (var sc : m_subContours) {
            sc.release();
        }
        if (!m_subContours.isEmpty()) m_subContours.clear();
        if (shape != null) shape.release();
    }
}

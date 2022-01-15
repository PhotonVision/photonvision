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
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;

public class SpeckleRejectPipe
        extends CVPipe<List<Contour>, List<Contour>, SpeckleRejectPipe.SpeckleRejectParams> {
    private final List<Contour> m_despeckledContours = new ArrayList<>();

    @Override
    protected List<Contour> process(List<Contour> in) {
        for (var c : m_despeckledContours) {
            c.mat.release();
        }
        m_despeckledContours.clear();

        if (in.size() > 0) {
            double averageArea = 0.0;
            for (Contour c : in) {
                averageArea += c.getArea();
            }
            averageArea /= in.size();

            double minAllowedArea = params.getMinPercentOfAvg() / 100.0 * averageArea;
            for (Contour c : in) {
                if (c.getArea() >= minAllowedArea) {
                    m_despeckledContours.add(c);
                } else {
                    c.release();
                }
            }
        }

        return m_despeckledContours;
    }

    public static class SpeckleRejectParams {
        private final double m_minPercentOfAvg;

        public SpeckleRejectParams(double minPercentOfAvg) {
            m_minPercentOfAvg = minPercentOfAvg;
        }

        public double getMinPercentOfAvg() {
            return m_minPercentOfAvg;
        }
    }
}

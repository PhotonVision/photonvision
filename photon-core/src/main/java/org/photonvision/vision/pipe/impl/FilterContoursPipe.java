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

import org.opencv.core.RotatedRect;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TargetCalculations;

public class FilterContoursPipe
        extends CVPipe<List<Contour>, List<Contour>, FilterContoursPipe.FilterContoursParams> {
    List<Contour> m_filteredContours = new ArrayList<>();

    @Override
    protected List<Contour> process(List<Contour> in) {
        m_filteredContours.clear();
        for (Contour contour : in) {
            filterContour(contour);
        }

        // we need the whole list for outlier rejection
        rejectOutliers(m_filteredContours, params.xTol, params.yTol);

        return m_filteredContours;
    }

    private void rejectOutliers(List<Contour> list, double xTol, double yTol) {
        if (list.size() < 2) return; // Must have at least 2 points to reject outliers

/*
        // Sort by X and find median
        list.sort(Comparator.comparingDouble(c -> c.getCenterPoint().x));

        double medianX = list.get(list.size() / 2).getCenterPoint().x;
        if (list.size() % 2 == 0)
            medianX = (medianX + list.get(list.size() / 2 - 1).getCenterPoint().x) / 2;
*/

        double meanX = list.stream().mapToDouble(it -> it.getCenterPoint().x).sum() / list.size();

        double stdDevX =
                list.stream().mapToDouble(it -> Math.pow(it.getCenterPoint().x - meanX, 2.0)).sum();
        stdDevX /= (list.size() - 1);
        stdDevX = Math.sqrt(stdDevX);

/*
        // Sort by Y and find median
        list.sort(Comparator.comparingDouble(c -> c.getCenterPoint().y));

        double medianY = list.get(list.size() / 2).getCenterPoint().y;
        if (list.size() % 2 == 0)
            medianY = (medianY + list.get(list.size() / 2 - 1).getCenterPoint().y) / 2;
*/

        double meanY = list.stream().mapToDouble(it -> it.getCenterPoint().y).sum() / list.size();

        double stdDevY =
                list.stream().mapToDouble(it -> Math.pow(it.getCenterPoint().y - meanY, 2.0)).sum();
        stdDevY /= (list.size() - 1);
        stdDevY = Math.sqrt(stdDevY);

        for (var it = list.iterator(); it.hasNext(); ) {
            // Reject points more than N standard devs above/below median
            // That is, |point - median| > std dev * tol
            Contour c = it.next();
            double x = c.getCenterPoint().x;
            double y = c.getCenterPoint().y;

            if (Math.abs(x - meanX) > stdDevX * xTol) {
                it.remove();
            } else if (Math.abs(y - meanY) > stdDevY * yTol) {
                it.remove();
            }
            // Otherwise we're good! Keep it in
        }
    }

    private void filterContour(Contour contour) {
        RotatedRect minAreaRect = contour.getMinAreaRect();

        // Area Filtering.
        double areaPercentage =
                minAreaRect.size.area() / params.getFrameStaticProperties().imageArea * 100.0;
        double minAreaPercentage = params.getArea().getFirst();
        double maxAreaPercentage = params.getArea().getSecond();
        if (areaPercentage < minAreaPercentage || areaPercentage > maxAreaPercentage) return;

        // Fullness Filtering.
        double contourArea = contour.getArea();
        double minFullness = params.getFullness().getFirst() * minAreaRect.size.area() / 100.0;
        double maxFullness = params.getFullness().getSecond() * minAreaRect.size.area() / 100.0;
        if (contourArea <= minFullness || contourArea >= maxFullness) return;

        // Aspect Ratio Filtering.
        double aspectRatio = TargetCalculations.getAspectRatio(contour.getMinAreaRect(), params.isLandscape);
        if (aspectRatio < params.getRatio().getFirst() || aspectRatio > params.getRatio().getSecond())
            return;

        m_filteredContours.add(contour);
    }

    public static class FilterContoursParams {
        private final DoubleCouple m_area;
        private final DoubleCouple m_ratio;
        private final DoubleCouple m_fullness;
        private final FrameStaticProperties m_frameStaticProperties;
        private final double xTol; // IQR tolerance for x
        private final double yTol; // IQR tolerance for x
        public final boolean isLandscape;

        public FilterContoursParams(
                DoubleCouple area,
                DoubleCouple ratio,
                DoubleCouple extent,
                FrameStaticProperties camProperties,
                double xTol,
                double yTol, boolean isLandscape) {
            this.m_area = area;
            this.m_ratio = ratio;
            this.m_fullness = extent;
            this.m_frameStaticProperties = camProperties;
            this.xTol = xTol;
            this.yTol = yTol;
            this.isLandscape = isLandscape;
        }

        public DoubleCouple getArea() {
            return m_area;
        }

        public DoubleCouple getRatio() {
            return m_ratio;
        }

        public DoubleCouple getFullness() {
            return m_fullness;
        }

        public FrameStaticProperties getFrameStaticProperties() {
            return m_frameStaticProperties;
        }
    }
}

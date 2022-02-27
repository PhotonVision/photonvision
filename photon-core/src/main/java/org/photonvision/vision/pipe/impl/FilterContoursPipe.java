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
import java.util.stream.Collectors;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;

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

        // the inter-quartile range is the value at the 75th percentile - 25th percentile
        List<Double> xCoords =
                list.stream().map(it -> it.getCenterPoint().x).collect(Collectors.toList());
        double p75 = MathUtils.getPercentile(xCoords, 75);
        double p25 = MathUtils.getPercentile(xCoords, 25);
        double iqr_x = p75 - p25; // Difference between 75th and 25th

        // Kill all further than N * the iqr in x
        for (var it = list.iterator(); it.hasNext(); ) {
            var x = it.next().getCenterPoint().x;

            // Remove points further than xTol * IQR above 75th, or below 25th
            if ((x > p75 + xTol * iqr_x) || (x < p25 - 1.5 * iqr_x)) {
                it.remove();
            }
        }

        // Repeat for Y. Maybe less to do this time!
        List<Double> yCoords =
                list.stream().map(it -> it.getCenterPoint().y).collect(Collectors.toList());
        p75 = MathUtils.getPercentile(yCoords, 75);
        p25 = MathUtils.getPercentile(yCoords, 25);
        double iqr_y = p75 - p25; // Difference between 75th and 25th

        // Kill all further than N * the iqr in y
        for (var it = list.iterator(); it.hasNext(); ) {
            var y = it.next().getCenterPoint().y;

            // Remove points further than xTol * IQR above 75th, or below 25th
            if ((y > p75 + yTol * iqr_x) || (y < p25 - 1.5 * iqr_y)) {
                it.remove();
            }
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
        double minFullness = params.getFullness().getFirst() * minAreaRect.size.area() / 100;
        double maxFullness = params.getFullness().getSecond() * minAreaRect.size.area() / 100;
        if (contourArea <= minFullness || contourArea >= maxFullness) return;

        // Aspect Ratio Filtering.
        Rect boundingRect = contour.getBoundingRect();
        double aspectRatio = (double) boundingRect.width / boundingRect.height;
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

        public FilterContoursParams(
                DoubleCouple area,
                DoubleCouple ratio,
                DoubleCouple extent,
                FrameStaticProperties camProperties,
                double xTol,
                double yTol) {
            this.m_area = area;
            this.m_ratio = ratio;
            this.m_fullness = extent;
            this.m_frameStaticProperties = camProperties;
            this.xTol = xTol;
            this.yTol = yTol;
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

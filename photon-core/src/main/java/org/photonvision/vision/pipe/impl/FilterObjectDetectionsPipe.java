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
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipe;

public class FilterObjectDetectionsPipe
        extends CVPipe<
                List<NeuralNetworkPipeResult>,
                List<NeuralNetworkPipeResult>,
                FilterObjectDetectionsPipe.FilterContoursParams> {
    List<NeuralNetworkPipeResult> m_filteredContours = new ArrayList<>();

    @Override
    protected List<NeuralNetworkPipeResult> process(List<NeuralNetworkPipeResult> in) {
        m_filteredContours.clear();
        for (var contour : in) {
            filterContour(contour);
        }

        return m_filteredContours;
    }

    private void filterContour(NeuralNetworkPipeResult contour) {
        var boc = contour.bbox;

        // Area filtering
        double areaPercentage = boc.area() / params.getFrameStaticProperties().imageArea * 100.0;
        double minAreaPercentage = params.getArea().getFirst();
        double maxAreaPercentage = params.getArea().getSecond();
        if (areaPercentage < minAreaPercentage || areaPercentage > maxAreaPercentage) return;

        // Aspect ratio filtering; much simpler since always axis-aligned
        double aspectRatio = boc.width / boc.height;
        if (aspectRatio < params.getRatio().getFirst() || aspectRatio > params.getRatio().getSecond())
            return;

        m_filteredContours.add(contour);
    }

    public static class FilterContoursParams {
        private final DoubleCouple m_area;
        private final DoubleCouple m_ratio;
        private final FrameStaticProperties m_frameStaticProperties;
        public final boolean isLandscape;

        public FilterContoursParams(
                DoubleCouple area,
                DoubleCouple ratio,
                FrameStaticProperties camProperties,
                boolean isLandscape) {
            this.m_area = area;
            this.m_ratio = ratio;
            this.m_frameStaticProperties = camProperties;
            this.isLandscape = isLandscape;
        }

        public DoubleCouple getArea() {
            return m_area;
        }

        public DoubleCouple getRatio() {
            return m_ratio;
        }

        public FrameStaticProperties getFrameStaticProperties() {
            return m_frameStaticProperties;
        }
    }
}

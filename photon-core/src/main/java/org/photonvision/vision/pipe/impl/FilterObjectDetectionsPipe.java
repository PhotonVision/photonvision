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
import org.photonvision.vision.target.TargetCalculations;

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
        var boc = contour.bbox();

        // Area filtering
        double areaPercentage = boc.size.area() / params.frameStaticProperties().imageArea * 100.0;
        double minAreaPercentage = params.area().getFirst();
        double maxAreaPercentage = params.area().getSecond();
        if (areaPercentage < minAreaPercentage || areaPercentage > maxAreaPercentage) return;

        // Aspect Ratio Filtering.
        double aspectRatio = TargetCalculations.getAspectRatio(boc, params.isLandscape());
        if (aspectRatio < params.ratio().getFirst() || aspectRatio > params.ratio().getSecond()) return;

        m_filteredContours.add(contour);
    }

    public static record FilterContoursParams(
            DoubleCouple area,
            DoubleCouple ratio,
            FrameStaticProperties frameStaticProperties,
            boolean isLandscape) {}
}

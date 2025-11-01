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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipe;

public class FilterObjectDetectionsPipe
        extends CVPipe<
                List<NeuralNetworkPipeResult>,
                List<NeuralNetworkPipeResult>,
                FilterObjectDetectionsPipe.FilterContoursParams> {
    private static final Logger logger =
            new Logger(FilterObjectDetectionsPipe.class, LogGroup.General);
    List<NeuralNetworkPipeResult> m_filteredContours = new ArrayList<>();

    @Override
    protected List<NeuralNetworkPipeResult> process(List<NeuralNetworkPipeResult> in) {
        m_filteredContours.clear();
        logger.debug(String.format("FilterObjectDetectionsPipe: processing %d detections", in.size()));
        logger.debug(
                String.format(
                        "  Area filter: %.2f%% - %.2f%%", params.area().getFirst(), params.area().getSecond()));
        logger.debug(
                String.format(
                        "  Ratio filter: %.2f - %.2f", params.ratio().getFirst(), params.ratio().getSecond()));

        for (var contour : in) {
            filterContour(contour);
        }

        logger.debug(
                String.format(
                        "FilterObjectDetectionsPipe: %d detections passed filters", m_filteredContours.size()));
        return m_filteredContours;
    }

    private void filterContour(NeuralNetworkPipeResult contour) {
        var boc = contour.bbox();

        // Area filtering
        double areaPercentage = boc.area() / params.frameStaticProperties().imageArea * 100.0;
        double minAreaPercentage = params.area().getFirst();
        double maxAreaPercentage = params.area().getSecond();

        logger.debug(
                String.format(
                        "  Detection: bbox=%.1fx%.1f, area=%.2f%%, areaRange=[%.2f, %.2f]",
                        boc.width, boc.height, areaPercentage, minAreaPercentage, maxAreaPercentage));

        if (areaPercentage < minAreaPercentage || areaPercentage > maxAreaPercentage) {
            logger.debug(
                    String.format(
                            "    REJECTED by area filter: %.2f%% not in [%.2f%%, %.2f%%]",
                            areaPercentage, minAreaPercentage, maxAreaPercentage));
            return;
        }

        // Aspect ratio filtering; much simpler since always axis-aligned
        double aspectRatio = boc.width / boc.height;

        logger.debug(
                String.format(
                        "    aspect=%.2f, ratioRange=[%.2f, %.2f]",
                        aspectRatio, params.ratio().getFirst(), params.ratio().getSecond()));

        if (aspectRatio < params.ratio().getFirst() || aspectRatio > params.ratio().getSecond()) {
            logger.debug(
                    String.format(
                            "    REJECTED by aspect ratio filter: %.2f not in [%.2f, %.2f]",
                            aspectRatio, params.ratio().getFirst(), params.ratio().getSecond()));
            return;
        }

        logger.debug("    PASSED all filters");
        m_filteredContours.add(contour);
    }

    public static record FilterContoursParams(
            DoubleCouple area,
            DoubleCouple ratio,
            FrameStaticProperties frameStaticProperties,
            boolean isLandscape) {}
}

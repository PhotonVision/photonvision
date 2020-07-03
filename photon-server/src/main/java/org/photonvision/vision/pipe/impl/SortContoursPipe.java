/*
 * Copyright (C) 2020 Photon Vision.
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
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.ContourSortMode;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.PotentialTarget;

public class SortContoursPipe
        extends CVPipe<
                List<PotentialTarget>, List<PotentialTarget>, SortContoursPipe.SortContoursParams> {

    private List<PotentialTarget> m_sortedContours = new ArrayList<>();

    @Override
    protected List<PotentialTarget> process(List<PotentialTarget> in) {
        m_sortedContours.clear();
        if (in.size() > 0) {
            m_sortedContours.addAll(in);
            if (params.getSortMode() != ContourSortMode.Centermost) {
                m_sortedContours.sort(params.getSortMode().getComparator());
            } else {
                m_sortedContours.sort(Comparator.comparingDouble(this::calcSquareCenterDistance));
            }
        }

        return new ArrayList<>(
                m_sortedContours.subList(0, Math.min(in.size(), params.getMaxTargets() - 1)));
    }

    private double calcSquareCenterDistance(PotentialTarget rect) {
        return FastMath.sqrt(
                FastMath.pow(params.getCamProperties().centerX - rect.getMinAreaRect().center.x, 2)
                        + FastMath.pow(params.getCamProperties().centerY - rect.getMinAreaRect().center.y, 2));
    }

    public static class SortContoursParams {
        private ContourSortMode m_sortMode;
        private FrameStaticProperties m_camProperties;
        private int m_maxTargets;

        public SortContoursParams(
                ContourSortMode sortMode, FrameStaticProperties camProperties, int maxTargets) {
            m_sortMode = sortMode;
            m_camProperties = camProperties;
            m_maxTargets = maxTargets;
        }

        public ContourSortMode getSortMode() {
            return m_sortMode;
        }

        public FrameStaticProperties getCamProperties() {
            return m_camProperties;
        }

        public int getMaxTargets() {
            return m_maxTargets;
        }
    }
}

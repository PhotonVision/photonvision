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
import java.util.Comparator;
import java.util.List;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.ContourSortMode;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.PotentialTarget;

public class SortContoursPipe
        extends CVPipe<
                List<PotentialTarget>, List<PotentialTarget>, SortContoursPipe.SortContoursParams> {
    private final List<PotentialTarget> m_sortedContours = new ArrayList<>();

    @Override
    protected List<PotentialTarget> process(List<PotentialTarget> in) {
        for (var oldTarget : m_sortedContours) {
            oldTarget.release();
        }
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
                m_sortedContours.subList(0, Math.min(in.size(), params.getMaxTargets())));
    }

    private double calcSquareCenterDistance(PotentialTarget rect) {
        return Math.sqrt(
                Math.pow(params.getCamProperties().centerX - rect.getMinAreaRect().center.x, 2)
                        + Math.pow(params.getCamProperties().centerY - rect.getMinAreaRect().center.y, 2));
    }

    public static class SortContoursParams {
        private final ContourSortMode m_sortMode;
        private final int m_maxTargets;
        private final FrameStaticProperties m_frameStaticProperties;

        public SortContoursParams(
                ContourSortMode sortMode, int maxTargets, FrameStaticProperties camProperties) {
            m_sortMode = sortMode;
            m_maxTargets = maxTargets;
            m_frameStaticProperties = camProperties;
        }

        public ContourSortMode getSortMode() {
            return m_sortMode;
        }

        public FrameStaticProperties getCamProperties() {
            return m_frameStaticProperties;
        }

        public int getMaxTargets() {
            return m_maxTargets;
        }
    }
}

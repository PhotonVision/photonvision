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
import java.util.Collections;
import java.util.List;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.PotentialTarget;

public class GroupContoursPipe
        extends CVPipe<List<Contour>, List<PotentialTarget>, GroupContoursPipe.GroupContoursParams> {
    private final List<PotentialTarget> m_targets = new ArrayList<>();

    @Override
    protected List<PotentialTarget> process(List<Contour> input) {
        for (var target : m_targets) {
            target.release();
        }

        m_targets.clear();

        if (params.getGroup() == ContourGroupingMode.Single) {
            for (var contour : input) {
                m_targets.add(new PotentialTarget(contour));
            }
        }
        // Check if we have at least 2 targets for 2 or more
        // This will only ever return 1 contour!
        else if (params.getGroup() == ContourGroupingMode.TwoOrMore
                && input.size() >= ContourGroupingMode.TwoOrMore.count) {
            // Just blob everything together
            Contour groupedContour = Contour.combineContourList(input);
            if (groupedContour != null) {
                m_targets.add(new PotentialTarget(groupedContour, input));
            }
        } else {
            int groupingCount = params.getGroup().count;

            if (input.size() >= groupingCount) {
                input.sort(Contour.SortByMomentsX);
                // also why reverse? shouldn't the sort comparator just get reversed?
                // TODO: Matt, see this
                Collections.reverse(input);

                for (int i = 0; i < input.size() - 1; i++) {
                    // make a list of the desired count of contours to group
                    // (Just make sure we don't get an index out of bounds exception
                    if (i < 0 || i + groupingCount > input.size()) continue;

                    // If we're in two or more mode, just try to group everything
                    List<Contour> groupingSet = input.subList(i, i + groupingCount);

                    try {
                        // FYI: This method only takes 2 contours!
                        Contour groupedContour =
                                Contour.groupContoursByIntersection(
                                        groupingSet.get(0), groupingSet.get(1), params.getIntersection());

                        if (groupedContour != null) {
                            m_targets.add(new PotentialTarget(groupedContour, groupingSet));
                            i += (groupingCount - 1);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return m_targets;
    }

    public static class GroupContoursParams {
        private final ContourGroupingMode m_group;
        private final ContourIntersectionDirection m_intersection;

        public GroupContoursParams(
                ContourGroupingMode group, ContourIntersectionDirection intersectionDirection) {
            m_group = group;
            m_intersection = intersectionDirection;
        }

        public ContourGroupingMode getGroup() {
            return m_group;
        }

        public ContourIntersectionDirection getIntersection() {
            return m_intersection;
        }
    }
}

package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.vision.opencv.Contour;
import com.chameleonvision.common.vision.pipeline.CVPipe;
import com.chameleonvision.common.vision.target.PotentialTarget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupContoursPipe
        extends CVPipe<List<Contour>, List<PotentialTarget>, GroupContoursPipe.GroupContoursParams> {

    private List<PotentialTarget> m_targets = new ArrayList<>();

    @Override
    protected List<PotentialTarget> process(List<Contour> input) {
        m_targets.clear();

        if (params.getGroup() == Contour.ContourGrouping.Single) {
            for (var contour : input) {
                m_targets.add(new PotentialTarget(contour));
            }
        } else {
            int groupingCount = params.getGroup().count;

            if (input.size() > groupingCount) {
                // todo: is it OK to mutate the input list?
                //  or should we clone it like before?
                //  what is the perf hit on cloning?
                input.sort(Contour.SortByMomentsX);
                // also why reverse? shouldn't the sort comparator just get reversed?
                Collections.reverse(input);
                // find out next time on Code Mysteries...

                for (int i = 0; i < input.size() - 1; i++) {
                    // make a list of the desired count of contours to group
                    List<Contour> groupingSet;
                    try {
                        groupingSet = input.subList(i, i + groupingCount - 1);
                    } catch (IndexOutOfBoundsException e) {
                        continue;
                    }

                    // FYI: This method only takes 2 contours!
                    Contour groupedContour =
                            Contour.groupContoursByIntersection(
                                    groupingSet.get(0), groupingSet.get(1), params.getIntersection());

                    if (groupedContour != null) {
                        m_targets.add(new PotentialTarget(groupedContour, groupingSet));
                    }
                }
            }
        }
        return m_targets;
    }

    public static class GroupContoursParams {
        private Contour.ContourGrouping m_group;
        private Contour.ContourIntersection m_intersection;

        public GroupContoursParams(
                Contour.ContourGrouping group, Contour.ContourIntersection intersection) {
            m_group = group;
            m_intersection = intersection;
        }

        public Contour.ContourGrouping getGroup() {
            return m_group;
        }

        public Contour.ContourIntersection getIntersection() {
            return m_intersection;
        }
    }
}

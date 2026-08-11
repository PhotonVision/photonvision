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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;

public class FindBoardCornersPipeTest {
    @Test
    void testSorting() {
        List<Integer> unsorted = new ArrayList<>(List.of(0, 1, 2, 3, 6, 8, 5, 4, 9, 7));
        List<Boolean> evenness =
                new ArrayList<>(List.of(true, false, true, false, true, true, false, true, false, false));
        List<Integer> sorted = new ArrayList<>(unsorted);
        Collections.sort(sorted);

        // This function expects that ids are a continuous range from 0 to n, already sorted prior to
        // the start index
        FindBoardCornersPipe.sortRelatedSubranges(unsorted, List.of(evenness), 4);

        assertEquals(sorted, unsorted);
        assertTrue(sorted.stream().allMatch(id -> evenness.get(id) == (id % 2 == 0)));
    }
}

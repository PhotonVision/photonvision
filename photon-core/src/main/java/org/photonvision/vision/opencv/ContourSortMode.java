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

package org.photonvision.vision.opencv;

import java.util.Comparator;
import org.photonvision.vision.target.PotentialTarget;

public enum ContourSortMode {
    Largest(
            Comparator.comparingDouble(PotentialTarget::getArea)
                    .reversed()), // reversed so that zero index has the largest size
    Smallest(Largest.getComparator().reversed()),
    Highest(Comparator.comparingDouble(rect -> rect.getMinAreaRect().center.y)),
    Lowest(Highest.getComparator().reversed()),
    Leftmost(Comparator.comparingDouble(target -> target.getMinAreaRect().center.x * -1)),
    Rightmost(Leftmost.getComparator().reversed()),
    Centermost(
            Comparator.comparingDouble(
                    rect ->
                            (Math.pow(rect.getMinAreaRect().center.y, 2)
                                    + Math.pow(rect.getMinAreaRect().center.x, 2))));

    private Comparator<PotentialTarget> m_comparator;

    ContourSortMode(Comparator<PotentialTarget> comparator) {
        m_comparator = comparator;
    }

    public Comparator<PotentialTarget> getComparator() {
        return m_comparator;
    }
}

package org.photonvision.vision.opencv;

import java.util.Comparator;
import org.apache.commons.math3.util.FastMath;
import org.photonvision.vision.target.PotentialTarget;

public enum ContourSortMode {
    Largest(Comparator.comparingDouble(PotentialTarget::getArea)),
    Smallest(Largest.getComparator().reversed()),
    Highest(Comparator.comparingDouble(rect -> rect.getMinAreaRect().center.y)),
    Lowest(Highest.getComparator().reversed()),
    Leftmost(Comparator.comparingDouble(target -> target.getMinAreaRect().center.x)),
    Rightmost(Leftmost.getComparator().reversed()),
    Centermost(
            Comparator.comparingDouble(
                    rect ->
                            (FastMath.pow(rect.getMinAreaRect().center.y, 2)
                                    + FastMath.pow(rect.getMinAreaRect().center.x, 2))));

    private Comparator<PotentialTarget> m_comparator;

    ContourSortMode(Comparator<PotentialTarget> comparator) {
        m_comparator = comparator;
    }

    public Comparator<PotentialTarget> getComparator() {
        return m_comparator;
    }
}

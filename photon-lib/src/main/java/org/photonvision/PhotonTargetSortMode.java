package org.photonvision;

import org.photonvision.targeting.PhotonTrackedTarget;

import java.util.Comparator;

public enum PhotonTargetSortMode {
    Smallest(Comparator.comparingDouble(PhotonTrackedTarget::getArea)),
    Largest(Smallest.m_comparator.reversed()),
    Highest(Comparator.comparingDouble(PhotonTrackedTarget::getPitch)),
    Lowest(Highest.m_comparator.reversed()),
    Rightmost(Comparator.comparingDouble(PhotonTrackedTarget::getYaw)),
    Leftmost(Rightmost.m_comparator.reversed()),
    Centermost(Comparator.comparingDouble(target -> (Math.pow(target.getPitch(), 2) + Math.pow(target.getYaw(), 2))));

    private final Comparator<PhotonTrackedTarget> m_comparator;

    PhotonTargetSortMode(Comparator<PhotonTrackedTarget> comparator) {
        m_comparator = comparator;
    }

    public Comparator<PhotonTrackedTarget> getComparator() {
        return m_comparator;
    }
}

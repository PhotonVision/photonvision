package org.photonvision.common.vision.opencv;

public enum ContourGroupingMode {
    Single(1),
    Dual(2);

    public final int count;

    ContourGroupingMode(int count) {
        this.count = count;
    }
}

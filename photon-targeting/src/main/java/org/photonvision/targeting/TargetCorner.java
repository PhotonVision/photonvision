package org.photonvision.targeting;

import java.util.Objects;

/**
 * Represents a point in an image at the corner of the minimum-area bounding rectangle, in pixels.
 * Origin at the top left, plus-x to the right, plus-y down.
 */
public class TargetCorner {
    public final double x;
    public final double y;

    public TargetCorner(double cx, double cy) {
        this.x = cx;
        this.y = cy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetCorner that = (TargetCorner) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

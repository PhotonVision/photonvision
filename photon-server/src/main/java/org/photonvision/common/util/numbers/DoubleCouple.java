package org.photonvision.common.util.numbers;

import org.opencv.core.Point;

public class DoubleCouple extends NumberCouple<Double> {

    public DoubleCouple() {
        super(0.0, 0.0);
    }

    public DoubleCouple(Double first, Double second) {
        super(first, second);
    }

    public DoubleCouple(Point point) {
        super(point.x, point.y);
    }

    public Point toPoint() {
        return new Point(first, second);
    }

    public void fromPoint(Point point) {
        first = point.x;
        second = point.y;
    }
}

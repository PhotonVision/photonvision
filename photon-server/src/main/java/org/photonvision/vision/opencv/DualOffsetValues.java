package org.photonvision.vision.opencv;

import org.opencv.core.Point;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.target.TargetCalculations;

public class DualOffsetValues {
    public final Point firstPoint;
    public final double firstPointArea;
    public final Point secondPoint;
    public final double secondPointArea;

    public DualOffsetValues() {
        firstPoint = new Point();
        firstPointArea = 0;
        secondPoint = new Point();
        secondPointArea = 0;
    }

    public DualOffsetValues(Point firstPoint, double firstPointArea, Point secondPoint, double secondPointArea) {
        this.firstPoint = firstPoint;
        this.firstPointArea = firstPointArea;
        this.secondPoint = secondPoint;
        this.secondPointArea = secondPointArea;
    }

    public DoubleCouple getLineValues() {
        return TargetCalculations.getLineFromPoints(firstPoint, secondPoint);
    }
}

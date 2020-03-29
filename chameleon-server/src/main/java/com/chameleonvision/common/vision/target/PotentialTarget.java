package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.vision.opencv.Contour;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class PotentialTarget {

    final Contour mainContour;
    final List<Contour> subContours = new ArrayList<>();

    public PotentialTarget(Contour inputContour) {
        mainContour = inputContour;
    }

    public PotentialTarget(
            List<Contour> subContours,
            TargetContourIntersection intersection,
            TargetContourGrouping grouping) {
        // do contour grouping
        mainContour = getGroupedContour(subContours, intersection, grouping);
        if (mainContour == null) {
            // this means we don't have a valid grouped target. what do we do???
            throw new RuntimeException("Something went fucky wucky");
        }
        this.subContours.addAll(subContours);
    }

    private Contour getGroupedContour(
            List<Contour> input, TargetContourIntersection intersection, TargetContourGrouping grouping) {
        int reqSize = grouping == TargetContourGrouping.Single ? 1 : 2;

        if (input.size() != reqSize) {
            return null;
            //            throw new RuntimeException("Insufficient contours for target grouping!");
        }

        switch (grouping) {
                // technically should never happen but :shrug:
            case Single:
                return input.get(0);
            case Dual:
                input.sort(Contour.SortByMomentsX);
                Collections.reverse(input); // why?

                Contour firstContour = input.get(0);
                Contour secondContour = input.get(1);

                // total contour for both. add the first one for now
                List<Point> fullContourPoints = new ArrayList<>(firstContour.mat.toList());

                // add second contour if it is intersecting
                if (firstContour.isIntersecting(secondContour, intersection)) {
                    fullContourPoints.addAll(secondContour.mat.toList());
                } else {
                    return null;
                }

                MatOfPoint finalContour = new MatOfPoint(fullContourPoints.toArray(new Point[0]));

                if (finalContour.cols() != 0 && finalContour.rows() != 0) {
                    return new Contour(finalContour);
                }
                break;
        }
        return null;
    }

    // TODO: move these? also docs plox
    public enum TargetContourIntersection {
        None,
        Up,
        Down,
        Left,
        Right
    }

    public enum TargetContourGrouping {
        Single,
        Dual
    }
}

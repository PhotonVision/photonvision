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

package org.photonvision.vision.pipe.impl;

import java.util.*;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TrackedTarget;

/**
 * Determines the target corners of the {@link TrackedTarget}. The {@link
 * CornerDetectionPipeParameters} affect how these corners are calculated.
 */
public class CornerDetectionPipe
        extends CVPipe<
                List<TrackedTarget>,
                List<TrackedTarget>,
                CornerDetectionPipe.CornerDetectionPipeParameters> {
    Comparator<Point> leftRightComparator = Comparator.comparingDouble(point -> point.x);
    Comparator<Point> verticalComparator = Comparator.comparingDouble(point -> point.y);
    MatOfPoint2f polyOutput = new MatOfPoint2f();

    @Override
    protected List<TrackedTarget> process(List<TrackedTarget> targetList) {
        for (var target : targetList) {
            // detect corners. Might implement more algorithms later but
            // APPROX_POLY_DP_AND_EXTREME_CORNERS should be year agnostic
            if (Objects.requireNonNull(params.cornerDetectionStrategy)
                    == DetectionStrategy.APPROX_POLY_DP_AND_EXTREME_CORNERS) {
                var targetCorners = detectExtremeCornersByApproxPolyDp(target, params.calculateConvexHulls);
                target.setTargetCorners(targetCorners);
            }
        }
        return targetList;
    }

    /**
     * @param target The target to find the corners of.
     * @return Corners: (bottom-left, bottom-right, top-right, top-left)
     */
    private List<Point> findBoundingBoxCorners(TrackedTarget target) {
        // extract the corners
        var points = new Point[4];
        target.m_mainContour.getMinAreaRect().points(points);

        // find the bl/br/tr/tl corners
        // first, min by left/right
        Arrays.sort(points, leftRightComparator);
        // of this, we now have left and right
        // sort to get top and bottom
        Point[] left = {points[0], points[1]};
        Arrays.sort(left, verticalComparator);
        Point[] right = {points[2], points[3]};
        Arrays.sort(right, verticalComparator);

        var tl = left[0];
        var bl = left[1];
        var tr = right[0];
        var br = right[1];

        return List.of(bl, br, tr, tl);
    }

    /**
     * @param a First point.
     * @param b Second point.
     * @return The straight line distance between them.
     */
    private static double distanceBetween(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    /**
     * Find the 4 most extreme corners of the target's contour.
     *
     * @param target The target to track.
     * @param convexHull Whether to use the convex hull of the contour instead.
     * @return The 4 extreme corners of the contour: (bottom-left, bottom-right, top-right, top-left)
     */
    private List<Point> detectExtremeCornersByApproxPolyDp(TrackedTarget target, boolean convexHull) {
        var centroid = target.getMinAreaRect().center;
        Comparator<Point> compareCenterDist =
                Comparator.comparingDouble((Point point) -> distanceBetween(centroid, point));

        MatOfPoint2f targetContour;
        if (convexHull) {
            targetContour = target.m_mainContour.getConvexHull();
        } else {
            targetContour = target.m_mainContour.getMat2f();
        }

        /*
        approximating a shape around the contours
        Can be tuned to allow/disallow hulls
        we want a number between 0 and 0.16 out of a percentage from 0 to 100
        so take accuracy and divide by 600

        Furthermore, we know that the contour is open if we haven't done convex hulls,
        and it has subcontours.
        */
        var isOpen = !convexHull && target.hasSubContours();
        var peri = Imgproc.arcLength(targetContour, true);
        Imgproc.approxPolyDP(
                targetContour, polyOutput, params.accuracyPercentage() / 600.0 * peri, !isOpen);

        // we must have at least 4 corners for this strategy to work.
        // If we are looking for an exact side count that is handled here too.
        var pointList = new ArrayList<>(polyOutput.toList());
        if (pointList.size() < 4 || (params.exactSideCount() && params.sideCount() != pointList.size()))
            return null;

        target.setApproximateBoundingPolygon(polyOutput);

        // left top, left bottom, right bottom, right top
        var boundingBoxCorners = findBoundingBoxCorners(target);

        var compareDistToTl =
                Comparator.comparingDouble((Point p) -> distanceBetween(p, boundingBoxCorners.get(3)));

        var compareDistToTr =
                Comparator.comparingDouble((Point p) -> distanceBetween(p, boundingBoxCorners.get(2)));

        // top left and top right are the poly corners closest to the bounding box tl and tr
        pointList.sort(compareDistToTl);
        var tl = pointList.remove(0);
        pointList.sort(compareDistToTr);
        var tr = pointList.remove(0);

        // at this point we look for points on the left/right of the center of the remaining points
        // and maximize their distance from the center of the min area rectangle
        var leftList = new ArrayList<Point>();
        var rightList = new ArrayList<Point>();
        double averageXCoordinate = 0.0;
        for (var p : pointList) {
            averageXCoordinate += p.x;
        }
        averageXCoordinate /= pointList.size();

        // add points that are below the center of the min area rectangle of the target
        for (var p : pointList) {
            if (p.y
                    > target.m_mainContour.getBoundingRect().y
                            + target.m_mainContour.getBoundingRect().height / 2.0) {
                if (p.x < averageXCoordinate) {
                    leftList.add(p);
                } else {
                    rightList.add(p);
                }
            }
        }
        if (leftList.isEmpty() || rightList.isEmpty()) return null;
        leftList.sort(compareCenterDist);
        rightList.sort(compareCenterDist);
        var bl = leftList.get(leftList.size() - 1);
        var br = rightList.get(rightList.size() - 1);
        return List.of(bl, br, tr, tl);
    }

    /**
     * @param accuracyPercentage Represents how "accurate" our approximate polygon must be.
     */
    public static record CornerDetectionPipeParameters(
            DetectionStrategy cornerDetectionStrategy,
            boolean calculateConvexHulls,
            boolean exactSideCount,
            int sideCount,
            double accuracyPercentage) {}

    public enum DetectionStrategy {
        APPROX_POLY_DP_AND_EXTREME_CORNERS
    }
}

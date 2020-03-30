package com.chameleonvision.common.vision.opencv;

import com.chameleonvision.common.util.math.MathUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class Contour {

    public static final Comparator<Contour> SortByMomentsX =
            Comparator.comparingDouble(
                    (contour) -> contour.getMoments().get_m10() / contour.getMoments().get_m00());

    public final MatOfPoint mat;

    private Double area = Double.NaN;
    private RotatedRect minAreaRect = null;
    private Rect boundingRect = null;
    private Moments moments = null;

    public Contour(MatOfPoint mat) {
        this.mat = mat;
    }

    public double getArea() {
        if (Double.isNaN(area)) {
            area = Imgproc.contourArea(mat);
        }
        return area;
    }

    public RotatedRect getMinAreaRect() {
        if (minAreaRect == null) {
            MatOfPoint2f temp = new MatOfPoint2f(mat.toArray());
            minAreaRect = Imgproc.minAreaRect(temp);
            temp.release();
        }
        return minAreaRect;
    }

    public Rect getBoundingRect() {
        if (boundingRect == null) {
            boundingRect = Imgproc.boundingRect(mat);
        }
        return boundingRect;
    }

    public Moments getMoments() {
        if (moments == null) {
            moments = Imgproc.moments(mat);
        }
        return moments;
    }

    public Point getCenterPoint() {
        return getMinAreaRect().center;
    }

    public boolean isEmpty() {
        return mat.cols() != 0 && mat.rows() != 0;
    }

    public boolean isIntersecting(Contour secondContour, ContourIntersection intersection) {
        boolean isIntersecting = false;

        if (intersection == ContourIntersection.None) {
            isIntersecting = true;
        } else {
            try {
                MatOfPoint2f intersectMatA = new MatOfPoint2f();
                MatOfPoint2f intersectMatB = new MatOfPoint2f();
                intersectMatA.fromArray(mat.toArray());
                intersectMatB.fromArray(secondContour.mat.toArray());
                RotatedRect a = Imgproc.fitEllipse(intersectMatA);
                RotatedRect b = Imgproc.fitEllipse(intersectMatB);
                double mA = MathUtils.toSlope(a.angle);
                double mB = MathUtils.toSlope(b.angle);
                double x0A = a.center.x;
                double y0A = a.center.y;
                double x0B = b.center.x;
                double y0B = b.center.y;
                double intersectionX = ((mA * x0A) - y0A - (mB * x0B) + y0B) / (mA - mB);
                double intersectionY = (mA * (intersectionX - x0A)) + y0A;
                double massX = (x0A + x0B) / 2;
                double massY = (y0A + y0B) / 2;
                switch (intersection) {
                    case Up:
                        if (intersectionY < massY) isIntersecting = true;
                        break;
                    case Down:
                        if (intersectionY > massY) isIntersecting = true;
                        break;
                    case Left:
                        if (intersectionX < massX) isIntersecting = true;
                        break;
                    case Right:
                        if (intersectionX > massX) isIntersecting = true;
                        break;
                }
                intersectMatA.release();
                intersectMatB.release();
            } catch (Exception e) {
                // defaults to false
            }
        }

        return isIntersecting;
    }

    // TODO: refactor to do "infinite" contours
    public static Contour groupContoursByIntersection(
            Contour firstContour, Contour secondContour, ContourIntersection intersection) {
        if (firstContour.isIntersecting(secondContour, intersection)) {
            return combineContours(firstContour, secondContour);
        } else {
            return null;
        }
    }

    // TODO: does this leak?
    private static Contour combineContours(Contour... contours) {
        List<Point> fullContourPoints = new ArrayList<>();

        for (var contour : contours) {
            fullContourPoints.addAll(contour.mat.toList());
        }

        var points = new MatOfPoint(fullContourPoints.toArray(new Point[0]));
        var finalContour = new Contour(points);

        if (!finalContour.isEmpty()) {
            return finalContour;
        } else return null;
    }

    // TODO: move these? also docs plox
    public enum ContourIntersection {
        None,
        Up,
        Down,
        Left,
        Right
    }

    public enum ContourGrouping {
        Single(1),
        Dual(2);

        public final int count;

        ContourGrouping(int count) {
            this.count = count;
        }
    }
}

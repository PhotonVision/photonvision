package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.util.MathHandler;
import com.chameleonvision.vision.enums.TargetGroup;
import com.chameleonvision.vision.enums.TargetIntersection;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GroupContoursPipe implements Pipe<List<MatOfPoint>, List<RotatedRect>> {

    private static final Comparator<MatOfPoint> sortByMomentsX =
            Comparator.comparingDouble(GroupContoursPipe::calcMomentsX);

    private TargetGroup group;
    private TargetIntersection intersection;

    private List<RotatedRect> groupedContours = new ArrayList<>();
    private MatOfPoint2f intersectMatA = new MatOfPoint2f();
    private MatOfPoint2f intersectMatB = new MatOfPoint2f();

    public GroupContoursPipe(TargetGroup group, TargetIntersection intersection) {
        this.group = group;
        this.intersection = intersection;
    }

    public void setConfig(TargetGroup group, TargetIntersection intersection) {
        this.group = group;
        this.intersection = intersection;
    }

    @Override
    public Pair<List<RotatedRect>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        List<MatOfPoint> sorted = new ArrayList<>(input);
        sorted.sort(sortByMomentsX);

        Collections.reverse(sorted);

        switch (group) {
            case Single: {
                input.forEach(c -> {
                    MatOfPoint2f contour = new MatOfPoint2f();
                    contour.fromArray(c.toArray());
                    if (contour.cols() != 0 && contour.rows() != 0) {
                        RotatedRect rect = Imgproc.minAreaRect(contour);
                        groupedContours.add(rect);
                    }
                });
                break;
            }
            case Dual: {
                for (var i = 0; i < input.size(); i++) {
                    List<Point> finalContourList = new ArrayList<>(input.get(i).toList());

                    try {
                        MatOfPoint firstContour = input.get(i);
                        MatOfPoint secondContour = input.get(i + 1);

                        if (isIntersecting(firstContour, secondContour)) {
                            finalContourList.addAll(secondContour.toList());
                        } else {
                            finalContourList.clear();
                            continue;
                        }

                        firstContour.release();
                        secondContour.release();

                        MatOfPoint2f contour = new MatOfPoint2f();
                        contour.fromList(finalContourList);

                        if (contour.cols() != 0 && contour.rows() != 0) {
                            RotatedRect rect = Imgproc.minAreaRect(contour);
                            groupedContours.add(rect);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println("GroupContours: WTF");
                        finalContourList.clear();
                    }
                }
                break;
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(groupedContours, processTime);
    }

    private static double calcMomentsX(MatOfPoint c) {
        Moments m = Imgproc.moments(c);
        return (m.get_m10() / m.get_m00());
    }

    private boolean isIntersecting(MatOfPoint contourOne, MatOfPoint contourTwo) {
        if (intersection.equals(TargetIntersection.None)) {
            return true;
        }

        try {
            intersectMatA.fromArray(contourOne.toArray());
            intersectMatB.fromArray(contourTwo.toArray());
            RotatedRect a = Imgproc.fitEllipse(intersectMatA);
            RotatedRect b = Imgproc.fitEllipse(intersectMatB);
            double mA = MathHandler.toSlope(a.angle);
            double mB = MathHandler.toSlope(b.angle);
            double x0A = a.center.x;
            double y0A = a.center.y;
            double x0B = b.center.x;
            double y0B = b.center.y;
            double intersectionX = ((mA * x0A) - y0A - (mB * x0B) + y0B) / (mA - mB);
            double intersectionY = (mA * (intersectionX - x0A)) + y0A;
            double massX = (x0A + x0B) / 2;
            double massY = (y0A + y0B) / 2;
            switch (intersection) {
                case Up: {
                    if (intersectionY < massY) {
                        if (mA > 0 && mB < 0) {
                            return true;
                        }
                    }
                    break;
                }
                case Down: {
                    if (intersectionY > massY) {
                        if (mA < 0 && mB > 0) {
                            return true;
                        }
                    }

                    break;
                }
                case Left: {
                    if (intersectionX < massX) {

                        return true;
                    }
                    break;
                }
                case Right: {
                    if (intersectionX > massX) {
                        return true;
                    }
                    break;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
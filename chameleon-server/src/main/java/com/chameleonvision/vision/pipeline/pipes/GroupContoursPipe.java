package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.util.MathHandler;
import com.chameleonvision.vision.enums.TargetGroup;
import com.chameleonvision.vision.enums.TargetIntersection;
import com.chameleonvision.vision.pipeline.Pipe;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GroupContoursPipe implements Pipe<List<MatOfPoint>, List<StandardCVPipeline.TrackedTarget>> {

    private static final Comparator<MatOfPoint> sortByMomentsX =
            Comparator.comparingDouble(GroupContoursPipe::calcMomentsX);

    private TargetGroup group;
    private TargetIntersection intersection;

    private MatOfPoint2f contourBuffer = new MatOfPoint2f();

    private List<StandardCVPipeline.TrackedTarget> groupedContours = new ArrayList<>();
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
    public Pair<List<StandardCVPipeline.TrackedTarget>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        groupedContours.forEach(StandardCVPipeline.TrackedTarget::release);
        groupedContours.clear();

        if (input.size() > (group.equals(TargetGroup.Single) ? 0 : 1)) {

            List<MatOfPoint> sorted = new ArrayList<>(input);
            sorted.sort(sortByMomentsX);

            Collections.reverse(sorted);

            switch (group) {
                case Single: {
                    input.forEach(c -> {
                        contourBuffer.fromArray(c.toArray());
                        if (contourBuffer.cols() != 0 && contourBuffer.rows() != 0) {
                            RotatedRect rect = Imgproc.minAreaRect(contourBuffer);
                            var target = new StandardCVPipeline.TrackedTarget();
                            target.minAreaRect = rect;
                            groupedContours.add(target);
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

                            intersectMatA.release();
                            intersectMatB.release();

                            contourBuffer.fromList(finalContourList);

                            if (contourBuffer.cols() != 0 && contourBuffer.rows() != 0) {
                                RotatedRect rect = Imgproc.minAreaRect(contourBuffer);
                                var target = new StandardCVPipeline.TrackedTarget();
                                target.minAreaRect = rect;

                                target.leftRightDualTargetPair =
                                        Pair.of(Imgproc.boundingRect(firstContour),
                                                Imgproc.boundingRect(secondContour));

                                tempRectMat.fromArray(firstContour.toArray());
                                var minAreaRect1 = Imgproc.minAreaRect(tempRectMat);
                                tempRectMat.fromArray(secondContour.toArray());
                                var minAreaRect2 = Imgproc.minAreaRect(tempRectMat);

                                target.leftRightRotatedRect =
                                        Pair.of(minAreaRect1, minAreaRect2);
                                
                                groupedContours.add(target);

                                firstContour.release();
                                secondContour.release();

                                // skip the next contour because it's been grouped already
                                i += 1;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            finalContourList.clear();
                        }
                    }
                    break;
                }
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(groupedContours, processTime);
    }

    MatOfPoint2f tempRectMat = new MatOfPoint2f();

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
                            return true;
                    }
                    break;
                }
                case Down: {
                    if (intersectionY > massY) {
                            return true;
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
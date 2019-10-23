package com.chameleonvision.vision.process;

import com.chameleonvision.vision.SortMode;
import com.chameleonvision.vision.TargetGroup;
import com.chameleonvision.vision.TargetIntersection;
import com.chameleonvision.vision.camera.CameraValues;
import com.chameleonvision.util.MathHandler;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class CVProcess {

    private final CameraValues cameraValues;
    private Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
    private Size blur = new Size(2, 2);
    private Mat hsvImage = new Mat();
    private List<MatOfPoint> foundContours = new ArrayList<>();
    private Mat binaryMat = new Mat();
    private List<MatOfPoint> filteredContours = new ArrayList<>();
    private Comparator<RotatedRect> sortByCentermostComparator = Comparator.comparingDouble(this::calcDistance);
    private List<MatOfPoint> speckleRejectedContours = new ArrayList<>();
    private Comparator<MatOfPoint> sortByMomentsX = Comparator.comparingDouble(this::calcMomentsX);
    private List<RotatedRect> finalCountours = new ArrayList<>();
    private MatOfPoint2f intersectMatA = new MatOfPoint2f();
    private MatOfPoint2f intersectMatB = new MatOfPoint2f();

    CVProcess(CameraValues cameraValues) {
        this.cameraValues = cameraValues;
    }

    void hsvThreshold(Mat srcImage, Mat dst, @NotNull Scalar hsvLower, @NotNull Scalar hsvUpper, boolean shouldErode, boolean shouldDilate) {
        Imgproc.cvtColor(srcImage, hsvImage, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.blur(hsvImage, hsvImage, blur);
        Core.inRange(hsvImage, hsvLower, hsvUpper, dst);
        if (shouldErode) {
            Imgproc.erode(dst, dst, kernel);
        }
        if (shouldDilate) {
            Imgproc.dilate(dst, dst, kernel);
        }
        hsvImage.release();
    }

    List<MatOfPoint> findContours(Mat src) {
        src.copyTo(binaryMat);
        foundContours.clear();
        Imgproc.findContours(binaryMat, foundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        binaryMat.release();
        return foundContours;
    }

    List<MatOfPoint> filterContours(List<MatOfPoint> inputContours, List<Number> area, List<Number> ratio, List<Number> extent) {
        for (MatOfPoint Contour : inputContours) {
            try {
                double contourArea = Imgproc.contourArea(Contour);
                double AreaRatio = (contourArea / cameraValues.ImageArea) * 100;
                double minArea = (MathHandler.sigmoid(area.get(0)));
                double maxArea = (MathHandler.sigmoid(area.get(1)));
                if (AreaRatio < minArea || AreaRatio > maxArea) {
                    continue;
                }
                var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));

                var targetFullness = contourArea;
                double minExtent = (double) (extent.get(0).doubleValue() * rect.size.area()) / 100;
                double maxExtent = (double) (extent.get(1).doubleValue() * rect.size.area()) / 100;
                if (targetFullness <= minExtent || contourArea >= maxExtent) {
                    continue;
                }
                Rect bb = Imgproc.boundingRect(Contour);
                double aspectRatio = (bb.width / bb.height);
                if (aspectRatio < ratio.get(0).doubleValue() || aspectRatio > ratio.get(1).doubleValue()) {
                    continue;
                }
                filteredContours.add(Contour);
            } catch (Exception e) {
                System.err.println("Error while filtering contours");
                e.printStackTrace();
            }
        }
        return filteredContours;
    }
    List<MatOfPoint> rejectSpeckles(List<MatOfPoint> inputContours, Double minimumPercentOfAverage) {
        double averageArea = 0.0;
        for(MatOfPoint c : inputContours) {
            averageArea += Imgproc.contourArea(c);
        }
        averageArea /= inputContours.size();
        var minimumAllowableArea = minimumPercentOfAverage / 100.0 * averageArea;
        speckleRejectedContours.clear();
        for(MatOfPoint c : inputContours) {
            if(Imgproc.contourArea(c) >= minimumAllowableArea) speckleRejectedContours.add(c);
        }
        return speckleRejectedContours;
    }


    private double calcDistance(RotatedRect rect) {
        return FastMath.sqrt(FastMath.pow(cameraValues.CenterX - rect.center.x, 2) + FastMath.pow(cameraValues.CenterY - rect.center.y, 2));
    }

    private double calcMomentsX(MatOfPoint c) {
        Moments m = Imgproc.moments(c);
        return (m.get_m10() / m.get_m00());
    }

    RotatedRect sortTargetsToOne(List<RotatedRect> inputRects, SortMode sortMode) {
        switch (sortMode) {
            case Largest:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case Smallest:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case Highest:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.y));
            case Lowest:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.y));
            case Leftmost:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.x));
            case Rightmost:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.x));
            case Centermost:
                return Collections.min(inputRects, sortByCentermostComparator);
            default:
                return inputRects.get(0); // default to whatever the first contour is, but this should never happen
        }
    }

    List<RotatedRect> groupTargets(List<MatOfPoint> inputContours, TargetIntersection intersectionPoint, TargetGroup targetGroup) {
        finalCountours.clear();
        if (targetGroup.equals(TargetGroup.Dual)) {
//            inputContours.sort(sortByMomentsX);
            for (var i = 0; i < inputContours.size(); i++) {
                List<Point> FinalContourList = new ArrayList<>(inputContours.get(i).toList());
                try {
                    MatOfPoint firstContour = inputContours.get(i);
                    MatOfPoint secondContour = inputContours.get(i + 1);
                    if (isIntersecting(firstContour, secondContour, intersectionPoint)) {
                        FinalContourList.addAll(secondContour.toList());
                    } else {
                        FinalContourList.clear();
                        break;
                    }
                    firstContour.release();
                    secondContour.release();
                    MatOfPoint2f contour = new MatOfPoint2f();
                    contour.fromList(FinalContourList);
                    if (contour.cols() != 0 && contour.rows() != 0) {
                        RotatedRect rect = Imgproc.minAreaRect(contour);
                        finalCountours.add(rect);
                    }
                } catch (IndexOutOfBoundsException e) {
                    FinalContourList.clear();
                    break;
                }
            }

        } else if (targetGroup.equals(TargetGroup.Single)) {
            for (MatOfPoint inputContour : inputContours) {
                MatOfPoint2f contour = new MatOfPoint2f();
                contour.fromArray(inputContour.toArray());
                if (contour.cols() != 0 && contour.rows() != 0) {
                    RotatedRect rect = Imgproc.minAreaRect(contour);
                    finalCountours.add(rect);
                }
            }
        }
        return finalCountours;
    }

    private boolean isIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, TargetIntersection intersectionPoint) {
        if (intersectionPoint.equals(TargetIntersection.None)) {
            return true;
        }
        try {
            intersectMatA.fromArray(ContourOne.toArray());
            intersectMatB.fromArray(ContourTwo.toArray());
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
            switch (intersectionPoint) {
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

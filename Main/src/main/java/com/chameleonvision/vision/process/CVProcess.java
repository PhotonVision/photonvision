package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.CameraValues;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class CVProcess {

    private final CameraValues CamVals;
    private HashMap<String, Integer> TargetGrouping = new HashMap<>() {{
        put("Single", 1);
        put("Dual", 2);
        put("Triple", 3);
        put("Quadruple", 4);
        put("Quintuple", 5);
    }};
    private Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
    private Mat hsvImage = new Mat();
    private List<MatOfPoint> FoundContours = new ArrayList<>();
    private Mat binaryMat = new Mat();
    private List<MatOfPoint> FilteredContours = new ArrayList<>();
    private Comparator<RotatedRect> SortByCentermostComparator = Comparator.comparingDouble(this::calcDistance);
    private List<RotatedRect> FinalCountours = new ArrayList<>();
    private Mat intersectMatA = new Mat();
    private Mat intersectMatB = new Mat();

    CVProcess(CameraValues camVals) {
        CamVals = camVals;
    }

    void HSVThreshold(Mat srcImage, Mat dst, @NotNull Scalar hsvLower, @NotNull Scalar hsvUpper, boolean shouldErode, boolean shouldDilate) {
        Imgproc.cvtColor(srcImage, hsvImage, Imgproc.COLOR_RGB2HSV, 3);
        Core.inRange(hsvImage, hsvLower, hsvUpper, dst);
        if (shouldErode) {
            Imgproc.erode(dst, dst, Kernel);
        }
        if (shouldDilate) {
            Imgproc.dilate(dst, dst, Kernel);
        }
        hsvImage.release();
    }

    List<MatOfPoint> FindContours(Mat src) {
        src.copyTo(binaryMat);
        FoundContours.clear();
        Imgproc.findContours(binaryMat, FoundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        binaryMat.release();
        return FoundContours;
    }

    List<MatOfPoint> FilterContours(List<MatOfPoint> InputContours, List<Integer> area, List<Integer> ratio, List<Integer> extent) {
        for (MatOfPoint Contour : InputContours) {
            try {
                var contourArea = Imgproc.contourArea(Contour);//TODO change scaling
                int targetArea = (int) ((((float) contourArea) / CamVals.ImageArea) * 100);
                if (targetArea < area.get(0) || targetArea > area.get(1)) {
                    continue;
                }
                var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));
                var targetFullness = (contourArea / rect.size.area()) * 100;
                if (targetFullness < extent.get(0) || targetArea > extent.get(1)) {
                    continue;
                }
                double aspectRatio = rect.size.width / rect.size.height;//TODO i think aspectRatio is inverted
                if (aspectRatio < ratio.get(0) || aspectRatio > ratio.get(1)) {
                    continue;
                }
                FilteredContours.add(Contour);
            } catch (Exception e) {
                System.err.println("Error while filtering contours");
                e.printStackTrace();
            }
        }
        return FilteredContours;
    }

    private double calcDistance(RotatedRect rect) {
        return FastMath.sqrt(FastMath.pow(CamVals.CenterX - rect.center.x, 2) + FastMath.pow(CamVals.CenterY - rect.center.y, 2));
    }

    RotatedRect SortTargetsToOne(List<RotatedRect> inputRects, String sortMode) {
        switch (sortMode) {
            case "Largest":
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case "Smallest":
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case "Highest":
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.y));
            case "Lowest":
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.y));
            case "Leftmost":
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.x));
            case "Rightmost":
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.x));
            case "Centermost":
                return Collections.min(inputRects, SortByCentermostComparator);
            default:
                return inputRects.get(0); // default to whatever the first contour is, but this should never happen
        }
    }

    List<RotatedRect> GroupTargets(List<MatOfPoint> InputContours, String IntersectionPoint, String TargetGroup) {
        FinalCountours.clear();
        if (!TargetGroup.equals("Single")) {
            for (var i = 0; i < InputContours.size(); i++) {
                List<Point> FinalContourList = new ArrayList<>(InputContours.get(i).toList());
                for (var c = 0; c < (TargetGrouping.get(TargetGroup) - 1); c++) {
                    try {
                        MatOfPoint firstContour = InputContours.get(i + c);
                        MatOfPoint secondContour = InputContours.get(i + c + 1);
                        if (IsIntersecting(firstContour, secondContour, IntersectionPoint)) {
                            FinalContourList.addAll(secondContour.toList());
                        }
                        else{
                            FinalContourList.clear();
                            break;
                        }
                        firstContour.release();
                        secondContour.release();
                        MatOfPoint2f contour = new MatOfPoint2f();
                        contour.fromList(FinalContourList);
                        if (contour.cols() != 0 && contour.rows() != 0) {
                            RotatedRect rect = Imgproc.minAreaRect(contour);
                            FinalCountours.add(rect);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        FinalContourList.clear();
                        break;
                    }
                }
            }

        } else {
            for (MatOfPoint inputContour : InputContours) {
                MatOfPoint2f contour = new MatOfPoint2f();
                contour.fromArray(inputContour.toArray());
                if (contour.cols() != 0 && contour.rows() != 0) {
                    RotatedRect rect = Imgproc.minAreaRect(contour);
                    FinalCountours.add(rect);
                }
            }
        }
        return FinalCountours;
    }

    private boolean IsIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, String IntersectionPoint) {
        if (IntersectionPoint.equals("None")) {
            return true;
        }
        try {
            Imgproc.fitLine(ContourOne, intersectMatA, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);
            Imgproc.fitLine(ContourTwo, intersectMatB, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);
            double vxA = intersectMatA.get(0, 0)[0];
            double vyA = intersectMatA.get(1, 0)[0];
            double x0A = intersectMatA.get(2, 0)[0];
            double y0A = intersectMatA.get(3, 0)[0];
            double mA = vyA / vxA;
            double vxB = intersectMatB.get(0, 0)[0];
            double vyB = intersectMatB.get(1, 0)[0];
            double x0B = intersectMatB.get(2, 0)[0];
            double y0B = intersectMatB.get(3, 0)[0];
            double mB = vyB / vxB;
            double bA = y0A - (mA*x0A);
            double bB = y0B - (mB*x0B);
            double intersectionX = ((mA * x0A) - y0A - (mB * x0B) + y0B )/ (mA - mB);
            double intersectionY = (mA * (intersectionX - x0A)) + y0A;
            double massX = intersectionX + 1;
            double massY = intersectionY + ((mA + bA + mB +bB) / 2);
            switch (IntersectionPoint) {
                case "Up": {
                    if (intersectionY < massY) {
                        return true;
                    }
                    break;
                }
                case "Down": {
                    if (intersectionY > massY) {
                        return true;
                    }
                    break;
                }
                case "Left": {
                    if (intersectionX < massX) {
                        return true;
                    }
                    break;
                }
                case "Right": {
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

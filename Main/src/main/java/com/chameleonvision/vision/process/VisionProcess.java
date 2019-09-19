package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.CameraValues;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.*;

public class VisionProcess {

    private HashMap<String, Integer>TargetGrouping= new HashMap<>() {{
        put("Single", 1);
        put("Dual", 2);
        put("Triple", 3);
        put("Quadruple", 4);
        put("Quintuple", 5);
    }};

    private final CameraValues CamVals;

    VisionProcess(CameraValues camVals){
        CamVals = camVals;
    }

    private Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

    private Mat hsvImage = new Mat();
    void HSVThreshold(Mat srcImage, Mat dst, @NotNull Scalar hsvLower, @NotNull Scalar hsvUpper, boolean shouldErode, boolean shouldDilate) {
        Imgproc.cvtColor(srcImage, hsvImage, Imgproc.COLOR_RGB2HSV,3);
        Core.inRange(hsvImage, hsvLower, hsvUpper, dst);
        if (shouldErode){
            Imgproc.erode(dst, dst, Kernel);
        }
        if (shouldDilate){
            Imgproc.dilate(dst, dst, Kernel);
        }
        hsvImage.release();
    }

    private List<MatOfPoint> FoundContours = new ArrayList<>();
    private Mat binaryMat = new Mat();
    List<MatOfPoint> FindContours(Mat src) {
        src.copyTo(binaryMat);
        FoundContours.clear();
        Imgproc.findContours(binaryMat, FoundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        binaryMat.release();
        return FoundContours;
    }

    private List<MatOfPoint> FilteredContours = new ArrayList<MatOfPoint>();
    List<MatOfPoint> FilterContours(List<MatOfPoint> InputContours, List<Integer> area, List<Integer> ratio, List<Integer> extent, String SortMode, String TargetIntersection, String TargetGrouping) {
        for (MatOfPoint Contour : InputContours){
            try{
                var contourArea = Imgproc.contourArea(Contour);
                double targetArea = (contourArea / CamVals.ImageArea) * 100;
                if (targetArea <= area.get(0) || targetArea >= area.get(1)){
                    continue;
                }
                var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));
                var targetFullness = (contourArea / rect.size.area()) * 100;
                if (targetFullness <= extent.get(0) || targetArea >= extent.get(1)){
                    continue;
                }
                var aspectRatio = rect.size.width / rect.size.height;
                if (aspectRatio <= ratio.get(0) || aspectRatio >= ratio.get(1)){
                    continue;
                }
                FilteredContours.add(Contour);
            }
            catch (Exception ignored) { }
        }
        return FilteredContours;
    }

    private static Comparator<RotatedRect> SortByLargestComparator = (rect1, rect2) -> Double.compare(rect2.size.area(), rect1.size.area());
    private static Comparator<RotatedRect> SortBySmallestComparator = SortByLargestComparator.reversed();

    private static Comparator<RotatedRect> SortByHighestComparator = (rect1, rect2) -> Double.compare(rect2.center.y, rect1.center.y);
    private static Comparator<RotatedRect> SortByLowestComparator = SortByHighestComparator.reversed();

    private static Comparator<RotatedRect> SortByLeftmostComparator = Comparator.comparingDouble(rect -> rect.center.x);
    private static Comparator<RotatedRect> SortByRightmostComparator = SortByLeftmostComparator.reversed();

    private double calcDistance(RotatedRect rect) {
        return FastMath.sqrt(FastMath.pow(CamVals.CenterX - rect.center.x, 2) + FastMath.pow(CamVals.CenterY - rect.center.y, 2));
    }

    private Comparator<RotatedRect> SortByCentermostComparator = Comparator.comparingDouble(this::calcDistance);

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
//                return inputRects.stream().sorted(SortByCentermostComparator).collect(Collectors.toList()).get(0);
            default:
                return inputRects.get(0); // default to whatever the first contour is, but this should never happen
        }
    }


    void SortTargets(List<RotatedRect> inputRects, String sortMode) {
        switch (sortMode) {
            case "Largest":
                inputRects.sort(SortByLargestComparator);
                break;
            case "Smallest":
                inputRects.sort(SortBySmallestComparator);
                break;
            case "Highest":
                inputRects.sort(SortByHighestComparator);
                break;
            case "Lowest":
                inputRects.sort(SortByLowestComparator);
                break;
            case "Leftmost":
                inputRects.sort(SortByLeftmostComparator);
                break;
            case "Rightmost":
                inputRects.sort(SortByRightmostComparator);
                break;
            case "Centermost":
                inputRects.sort(SortByCentermostComparator);
                break;
            default:
                break;
        }
    }

    private List<RotatedRect> FinalCountours = new ArrayList<>();
    List<RotatedRect> GroupTargets(List<MatOfPoint> InputContours, String IntersectionPoint, String TargetGroup) {
        FinalCountours.clear();
        if (!TargetGroup.equals("Single")){
            for (var i = 0; i < InputContours.size(); i++){
                List<Point> FinalContourList = new ArrayList<>(InputContours.get(i).toList());
                for (var c = 0; c < (TargetGrouping.get(TargetGroup) - 1); c++){
                    try{
                        MatOfPoint firstContour = InputContours.get(i + c);
                        MatOfPoint secondContour = InputContours.get(i + c + 1);
                        if (IsIntersecting(firstContour, secondContour, IntersectionPoint)){
                            FinalContourList.addAll(secondContour.toList());
                        }
                        firstContour.release();
                        secondContour.release();
                        MatOfPoint2f contour = new MatOfPoint2f();
                        contour.fromList(FinalContourList);
                        if (contour.cols() != 0 && contour.rows() != 0){
                            RotatedRect rect = Imgproc.minAreaRect(contour);
                            FinalCountours.add(rect);
                        }
                    } catch (IndexOutOfBoundsException e){
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

    private Mat intersectMatA = new Mat();
    private Mat intersectMatB = new Mat();
    private boolean IsIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, String IntersectionPoint) {
        if (IntersectionPoint.equals("None")){
            return true;
        }
        try {
            Imgproc.fitLine(ContourOne, intersectMatA, Imgproc.CV_DIST_L2,0,0.01,0.01);
            Imgproc.fitLine(ContourTwo, intersectMatB, Imgproc.CV_DIST_L2,0,0.01,0.01);
            double vxA = intersectMatA.get(0,0)[0];
            double vyA = intersectMatA.get(1,0)[0];
            double x0A = intersectMatA.get(2,0)[0];
            double y0A = intersectMatA.get(3,0)[0];
            double mA = vyA / vxA;
            double vxB = intersectMatB.get(0,0)[0];
            double vyB = intersectMatB.get(1,0)[0];
            double x0B = intersectMatB.get(2,0)[0];
            double y0B = intersectMatB.get(3,0)[0];
            double mB = vyB / vxB;
            double intersectionX = (mA * x0A) - y0A - (mB * x0B) + y0B / (mA - mB);
            double intersectionY = (mA * (intersectionX - x0A)) + y0A;
            switch (IntersectionPoint){
                case "Up" :{
                    if (intersectionY < CamVals.CenterY){
                        return true;
                    }
                    break;
                }
                case "Down": {
                    if (intersectionY > CamVals.CenterY){
                        return true;
                    }
                    break;
                }
                case "Left": {
                    if (intersectionX < CamVals.CenterX){
                        return true;
                    }
                    break;
                }
                case "Right": {
                    if (intersectionX > CamVals.CenterX){
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
        catch (Exception e){
            return false;
        }
    }
}

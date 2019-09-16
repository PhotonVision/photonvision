package com.chameleonvision.vision.process;

import com.chameleonvision.vision.CameraValues;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                var targetFullness = (contourArea/rect.size.area())*100;
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

    private List<MatOfPoint> FinalCountours = new ArrayList<>();
    private List<MatOfPoint> GroupTargets(List<MatOfPoint> InputContours, String IntersectionPoint, String TargetGroup) {
        FinalCountours.clear();
        if (!TargetGroup.equals("Single")){
            for (var i = 0; i < InputContours.size(); i++){
                var FinalContour = InputContours.get(i);
                for (var c = 0; c < (TargetGrouping.get(TargetGroup)-1);c++){
                    try{
                        MatOfPoint firstContour = InputContours.get(i + c);
                        MatOfPoint secondContour = InputContours.get(i+c+1);
                        if (IsIntersecting(firstContour, secondContour, IntersectionPoint)){
                            System.out.println("");
                        }
                        firstContour.release();
                        secondContour.release();
                    } catch (IndexOutOfBoundsException e){
                        FinalContour = new MatOfPoint();
                        break;
                    }
                }
            }

        }
        return InputContours;
    }

    private Mat intersectMatA = new Mat();
    private Mat intersectMatB = new Mat();
    private boolean IsIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, String IntersectionPoint) {
        Imgproc.fitLine(ContourOne, intersectMatA, Imgproc.CV_DIST_L2,0,0.01,0.01);
        Imgproc.fitLine(ContourTwo, intersectMatB, Imgproc.CV_DIST_L2,0,0.01,0.01);

        return true;
    }
}

package com.chameleonvision.vision.process;

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

    private double CamArea, CenterX, CenterY;

    VisionProcess(double CenterX, double CenterY, double CamArea){
        this.CenterX = CenterX;
        this.CenterY = CenterY;
        this.CamArea = CamArea;
    }

    private Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

    private Mat hsvMat = new Mat();
    private Mat hsvThreshMat = new Mat();
    private Scalar hsvLower, hsvUpper;

    Mat HSVThreshold(@NotNull List<Integer> hue, @NotNull List<Integer> saturation, @NotNull List<Integer> value, Mat image, boolean IsErode, boolean IsDilate){
        Imgproc.cvtColor(image, hsvMat,Imgproc.COLOR_BGR2HSV,3);
        hsvLower = new Scalar(hue.get(0), saturation.get(0), value.get(0));
        hsvUpper = new Scalar(hue.get(1), saturation.get(1), value.get(1));
        Core.inRange(hsvMat, hsvLower, hsvUpper, hsvThreshMat);
        if (IsErode){
            Imgproc.erode(hsvThreshMat, hsvThreshMat, Kernel);
        }
        if (IsDilate){
            Imgproc.dilate(hsvThreshMat, hsvThreshMat, Kernel);
        }
        return hsvThreshMat;
    }

    private List<MatOfPoint> FoundContours = new ArrayList<>();
    public List<MatOfPoint> FindContours(Mat BinaryImage){
        FoundContours.clear();
        Imgproc.findContours(BinaryImage, FoundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        BinaryImage.release();
        return FoundContours;
    }

    private List<MatOfPoint> FilteredContours = new ArrayList<MatOfPoint>();
    public List<MatOfPoint> FilterContours(List<MatOfPoint> InputContours, List<Integer> area, List<Integer> ratio, List<Integer> extent, String SortMode, String TargetIntersection, String TargetGrouping){
        for (MatOfPoint Contour : InputContours){
            try{
                var contourArea = Imgproc.contourArea(Contour);
                double targetArea = (contourArea / CamArea) * 100;
                if (targetArea >= area.get(0) || targetArea <= area.get(1)){
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
            catch (Exception e) {

            }
        }
        return FilteredContours;
    }

    private List<MatOfPoint> FinalCountours = new ArrayList<>();
    private List<MatOfPoint> GroupTargets(List<MatOfPoint> InputContours, String IntersectionPoint,String TargetGroup) {
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
//        Rect2d =
        return true;
    }
}

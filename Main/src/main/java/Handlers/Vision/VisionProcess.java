package Handlers.Vision;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisionProcess {
    private HashMap<String, Integer>TargetGrouping= new HashMap<String, Integer>(){{
        put("Single",1);
        put("Dual",2);
        put("Triple",3);
        put("Quadruple",4);
        put("Quintuple",5);
    }};
    private double CamArea,CenterX, CenterY;
    VisionProcess(double CenterX, double CenterY, double CamArea){
        this.CenterX = CenterX;
        this.CenterY = CenterY;
        this.CamArea = CamArea;
    }
    private Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

    public Mat HSVThreshold(@NotNull List<Integer> hue, @NotNull List<Integer> saturation , @NotNull List<Integer> value, Mat image, boolean IsErode, boolean IsDilate){
        Mat hsv = new Mat();
        Imgproc.cvtColor(image,hsv,Imgproc.COLOR_BGR2HSV,3);
        new Scalar(hue.get(0),saturation.get(0),value.get(0));
        Mat threshold = new Mat();
        Core.inRange(hsv,new Scalar(hue.get(0),saturation.get(0),value.get(0)),new Scalar(hue.get(1),saturation.get(1),value.get(1)),threshold);
        if (IsErode){
            Imgproc.erode(threshold,threshold, Kernel);
        }
        if (IsDilate){
            Imgproc.dilate(threshold,threshold, Kernel);
        }
        return threshold;
    }
    public List<MatOfPoint> FindContours(Mat BinaryImage){
        List<MatOfPoint> Contours = new ArrayList<>();
        Imgproc.findContours(BinaryImage,Contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_TC89_L1);
        return Contours;
    }
    public List<MatOfPoint> FilterContours(List<MatOfPoint> InputContours, List<Integer> area, List<Integer> ratio, List<Integer> extent, String SortMode,String TargetIntersection , String TargetGrouping){
        List<MatOfPoint> FilteredContours = new ArrayList<MatOfPoint>();
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
            } catch (Exception e){
                continue;
            }
        }
        return FilteredContours;
    }
    private List<MatOfPoint> GroupTargets(List<MatOfPoint> InputContours, String IntersectionPoint,String TargetGroup){
        if (!TargetGroup.equals("Single")){
            List<MatOfPoint> FinalCountours = new ArrayList<MatOfPoint>();
            for (var i = 0; i < InputContours.size(); i++){
                var FinalContour = InputContours.get(i);
                for (var c = 0; c < (TargetGrouping.get(TargetGroup)-1);c++){
                    try{
                        MatOfPoint firstContour = InputContours.get(i + c);
                        MatOfPoint secoundContour = InputContours.get(i+c+1);
                        if (IsIntersecting(firstContour,secoundContour, IntersectionPoint)){
                            System.out.println("");
                        }
                    } catch (IndexOutOfBoundsException e){
                        FinalContour = new MatOfPoint();
                        break;
                    }
                }
            }

        }
        return InputContours;
    }
    private boolean IsIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, String IntersectionPoint){
        Mat LineA = new Mat();
        Imgproc.fitLine(ContourOne,LineA,Imgproc.CV_DIST_L2,0,0.01,0.01);
        Mat LineB = new Mat();
        Imgproc.fitLine(ContourTwo,LineB,Imgproc.CV_DIST_L2,0,0.01,0.01);
        return true;
    }

}

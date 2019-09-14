package Handlers.Vision;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.ArrayList;
import java.util.List;

public class VisionProcess {
    private Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

    public Mat HSVThreshold(@NotNull List<Integer> hue, @NotNull List<Integer> saturation , @NotNull List<Integer> value, Mat image, boolean IsErode, boolean IsDilate){
        Mat hsv = new Mat();
        Imgproc.cvtColor(image,hsv,Imgproc.COLOR_BGR2HSV,3);
        new Scalar(hue.get(0),saturation.get(0),value.get(0));
        Mat threshold = new Mat();
        Core.inRange(threshold,new Scalar(hue.get(0),saturation.get(0),value.get(0)),new Scalar(hue.get(1),saturation.get(1),value.get(1)),threshold);
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
}

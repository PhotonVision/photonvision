package org.photonvision.vision.calibration;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Size;


public class Calib3dorFisheye {
    static boolean isFisheye(Mat distCoeffs) {
        return distCoeffs.size(1) == 4;
    }

    public static void projectPoints(MatOfPoint3f objectPoints, Mat rvec, Mat tvec, Mat cameraMatrix, MatOfDouble distCoeffs, MatOfPoint2f imagePoints){
        if(isFisheye(distCoeffs)){
            Calib3d.fisheye_projectPoints(objectPoints, imagePoints, rvec, tvec, cameraMatrix, distCoeffs);
        }
        else{
            Calib3d.projectPoints(objectPoints, rvec, tvec, cameraMatrix, distCoeffs, imagePoints);
        }
    }

    public static void undistortPoints(Mat src, Mat dst, Mat cameraMatrix, Mat distCoeffs) {
        if(isFisheye(distCoeffs)){
            Calib3d.fisheye_undistortPoints(src, dst, cameraMatrix, distCoeffs, new Mat(), cameraMatrix);
        }
        else{
            Calib3d.undistortPoints((MatOfPoint2f)src, (MatOfPoint2f)dst, cameraMatrix, distCoeffs, new Mat(), cameraMatrix);
        }
    }

    public static void distortPoints(MatOfPoint2f src, MatOfPoint2f dst, Mat cameraMatrix, Mat distCoeffs) {
        if(isFisheye(distCoeffs)){
            var rvec = new Mat();
            var tvec = new Mat();

            var src_points3d = new Mat();
            Calib3d.convertPointsToHomogeneous(src, src_points3d);
            Core.transpose(src_points3d, src_points3d);
            Calib3d.fisheye_projectPoints(src_points3d, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), dst);
        }
        else{
            var rvec = new Mat();
            var tvec = new Mat();

            var src_points3d = new Mat();
            Calib3d.convertPointsToHomogeneous(src, src_points3d);
            Calib3d.projectPoints(new MatOfPoint3f(src_points3d), rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), dst);
        }
    }

    public static void solvePnP(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat cameraMatrix, MatOfDouble distCoeffs, Mat rvec, Mat tvec) {
        if(isFisheye(distCoeffs)){
            MatOfPoint2f undistortedImagePoints = new MatOfPoint2f();
            Calib3d.fisheye_undistortPoints(imagePoints, undistortedImagePoints, cameraMatrix, distCoeffs);
            Calib3d.solvePnP(objectPoints, undistortedImagePoints, Mat.eye(new Size(3, 3), CvType.CV_8UC1), new MatOfDouble(Mat.zeros(new Size(1,5), CvType.CV_64F)), rvec, tvec);
        }
        else{
            Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec);
        }
    }

    public static double calibrateCamera(List<Mat> objectPoints, List<Mat> imagePoints, Size imageSize, Mat cameraMatrix, Mat distCoeffs, List<Mat> rvecs, List<Mat> tvecs){
        if(isFisheye(distCoeffs)){
            return Calib3d.fisheye_calibrate(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs);
        }
        else{
            return Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs);
        }
    }
}

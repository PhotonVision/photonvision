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
import org.opencv.core.Point3;
import org.opencv.core.Size;


public class Calib3dorFisheye {
    static boolean isFisheye(Mat distCoeffs) {
        return distCoeffs.size(1) == 4;
    }

    public static void projectPoints(MatOfPoint3f objectPoints, Mat rvec, Mat tvec, Mat cameraMatrix, Mat distCoeffs, MatOfPoint2f imagePoints){
        if(isFisheye(distCoeffs)){
            Calib3d.fisheye_projectPoints(objectPoints, imagePoints, rvec, tvec, cameraMatrix, distCoeffs);
        }
        else{
            Calib3d.projectPoints(objectPoints, rvec, tvec, cameraMatrix, new MatOfDouble(distCoeffs), imagePoints);
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

    public static void old_distortPoints(MatOfPoint2f src, MatOfPoint2f dst, Mat cameraMatrix, Mat distCoeffs) {
        var pointsList = src.toList();
        var dstList = new ArrayList<Point>();
        var cx = cameraMatrix.get(0, 2)[0];
        var cy = cameraMatrix.get(1, 2)[0];
        var fx = cameraMatrix.get(0, 0)[0];
        var fy = cameraMatrix.get(1, 1)[0];
        var k1 = distCoeffs.get(0, 0)[0];
        var k2 = distCoeffs.get(0, 1)[0];
        var k3 = distCoeffs.get(0, 4)[0];
        var p1 = distCoeffs.get(0, 2)[0];
        var p2 = distCoeffs.get(0, 3)[0];

        for (Point point : pointsList) {
            // To relative coordinates <- this is the step you are missing.
            double x = (point.x - cx) / fx; // cx, cy is the center of distortion
            double y = (point.y - cy) / fy;

            double r2 = x * x + y * y; // square of the radius from center

            // Radial distorsion
            double xDistort = x * (1 + k1 * r2 + k2 * r2 * r2 + k3 * r2 * r2 * r2);
            double yDistort = y * (1 + k1 * r2 + k2 * r2 * r2 + k3 * r2 * r2 * r2);

            // Tangential distorsion
            xDistort = xDistort + (2 * p1 * x * y + p2 * (r2 + 2 * x * x));
            yDistort = yDistort + (p1 * (r2 + 2 * y * y) + 2 * p2 * x * y);

            // Back to absolute coordinates.
            xDistort = xDistort * fx + cx;
            yDistort = yDistort * fy + cy;
            dstList.add(new Point(xDistort, yDistort));
        }
        dst.fromList(dstList);
    }

    public static void distortPoints(MatOfPoint2f src, MatOfPoint2f dst, Mat cameraMatrix, Mat distCoeffs) {
        var srcPtsList = src.toList();
        var srcPtsListNormalized = new ArrayList<Point3>();

        var cx = cameraMatrix.get(0, 2)[0];
        var cy = cameraMatrix.get(1, 2)[0];
        var fx = cameraMatrix.get(0, 0)[0];
        var fy = cameraMatrix.get(1, 1)[0];

        for (Point point : srcPtsList) {
            // To relative coordinates <- this is the step you are missing.
            double x = (point.x - cx) / fx; // cx, cy is the center of distortion
            double y = (point.y - cy) / fy;
            
            srcPtsListNormalized.add(new Point3(x, y, 1));
        }

        var srcPtsNormalized = new MatOfPoint3f();
        srcPtsNormalized.fromList(srcPtsListNormalized);

        var rvec = Mat.zeros(3, 1, CvType.CV_64FC1);
        var tvec = Mat.zeros(3, 1, CvType.CV_64FC1);

        Calib3dorFisheye.projectPoints(srcPtsNormalized, rvec, tvec, cameraMatrix, distCoeffs, dst);
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

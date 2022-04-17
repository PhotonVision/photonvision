package org.photonvision.vision.apriltag;

import java.util.List;

import org.opencv.core.Mat;

public class AprilTagJNI {
    // Returns a pointer to a apriltag_detector_t
    public static native long AprilTag_Create(String fam,
        double decimate, double blur, int threads, boolean debug,
        boolean refine_edges);

    private static native Object[] AprilTag_Detect(long detector, long imgAddr, int rows, int cols);

    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static DetectionResult[] AprilTag_Detect(long detector, Mat img) {
        return (DetectionResult[]) AprilTag_Detect(detector, img.dataAddr(), img.rows(), img.cols());
    }

    public static void main(String[] args) {
        System.loadLibrary("apriltag");

        long detector = AprilTag_Create("tag36h11", 2, 0, 1, false, true);

        // var buff = ByteBuffer.allocateDirect(1280 * 720);

        // // try {
        // //     CameraServerCvJNI.forceLoad();
        // // } catch (IOException e) {
        // //     // TODO Auto-generated catch block
        // //     e.printStackTrace();
        // // }
        // // PicamJNI.forceLoad();
        // // TestUtils.loadLibraries();
        // var img = Imgcodecs.imread("~/Downloads/TagFams.jpg");

        var ret = AprilTag_Detect(detector, 0, 720, 1280);
        // System.out.println(detector);
        // System.out.println(ret);
        System.out.println(List.of(ret));
    }
}
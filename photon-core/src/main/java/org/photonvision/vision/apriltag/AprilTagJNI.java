package org.photonvision.vision.apriltag;

public class AprilTagJNI {

    public class AprilTagDetection {
        int id;
        int hamming;
        float decision_margin;
        double[] homography;
        double centerX, centerY;
        double[] corners;
    }

    // Returns a pointer to a apriltag_detector_t
    public static native long AprilTag_Create(String fam, 
        double decimate, double blur, int threads, boolean debug,
        boolean refine_edges);

    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static native AprilTagDetection[] AprilTag_Detect(long detector, long pMat);
}
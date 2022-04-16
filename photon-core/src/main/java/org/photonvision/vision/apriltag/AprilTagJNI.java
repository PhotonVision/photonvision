package org.photonvision.vision.apriltag;

import org.opencv.core.Mat;

public class AprilTagJNI {



    // Returns a pointer to a apriltag_detector_t
    public static native long AprilTag_Create(String fam, 
        double decimate, double blur, int threads, boolean debug,
        boolean refine_edges);
    
    public static native DetectionResult[] AprilTag_Detect(long detector, long imgAddr, int rows, int cols);
    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static DetectionResult[] AprilTag_Detect(long detector, Mat img) {
        return AprilTag_Detect(detector, img.dataAddr(), img.rows(), img.cols());
    }
}
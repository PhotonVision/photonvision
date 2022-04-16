package org.photonvision.vision.apriltag;

public class DetectionResult{
    int id;
    int hamming;
    float decision_margin;
    double[] homography;
    double centerX, centerY;
    double[] corners;

    public DetectionResult(int id, int hamming, float decision, double[] homography,
            double cx, double cy, double[] corners) {

    }
}

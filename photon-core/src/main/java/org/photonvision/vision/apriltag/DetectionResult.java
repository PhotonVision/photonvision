package org.photonvision.vision.apriltag;

public class DetectionResult{
    public int getId() {
        return id;
    }

    public int getHamming() {
        return hamming;
    }

    public float getDecisionMargin() {
        return decision_margin;
    }

    public void setDecisionMargin(float decision_margin) {
        this.decision_margin = decision_margin;
    }

    public double[] getHomography() {
        return homography;
    }

    public void setHomography(double[] homography) {
        this.homography = homography;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double[] getCorners() {
        return corners;
    }

    public void setCorners(double[] corners) {
        this.corners = corners;
    }

    int id;
    int hamming;
    float decision_margin;
    double[] homography;
    double centerX, centerY;
    double[] corners;

    public DetectionResult(int id, int hamming, float decision_margin, double[] homography, double centerX,
            double centerY, double[] corners) {
        this.id = id;
        this.hamming = hamming;
        this.decision_margin = decision_margin;
        this.homography = homography;
        this.centerX = centerX;
        this.centerY = centerY;
        this.corners = corners;
    }

    @Override
    public String toString() {
        return "ID " + id + " ham " + hamming + " decision margin " + decision_margin
                + " homography " + homography + " cx " + centerX + " cy " + centerY
                + " corners " + corners;
    }
}

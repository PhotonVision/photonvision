package org.photonvision.vision.aruco;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.apriltag.AprilTagDetector;

import java.util.Arrays;
import java.util.List;

public class ArucoDetectionResult {
    private static final Logger logger = new Logger(ArucoDetectionResult.class, LogGroup.VisionModule);
    double[] xCorners;
    double[] yCorners;

    double centerX;
    double centerY;
    int id;

    Pose3d pose;
    public ArucoDetectionResult(double[] xCorners, double[] yCorners, int id, Pose3d pose) {

        this.xCorners = xCorners;
        this.yCorners = yCorners;
        this.centerX = centerX;
        this.centerY = centerY;
        this.id = id;
        this.pose = pose;
        logger.debug("Creating a new detection result: " + this.toString());
    }

    public Pose3d getPose() {
        return pose;
    }

    public double[] getxCorners() {
        return xCorners;
    }

    public double[] getyCorners() {
        return yCorners;
    }

    public int getId() {
        return id;
    }

    public double getCenterX() {
        return (xCorners[0]+xCorners[1]+xCorners[2]+xCorners[3])*.25;
    }
    public double getCenterY() {
        return (yCorners[0]+yCorners[1]+yCorners[2]+yCorners[3])*.25;
    }

    @Override
    public String toString() {
        return "ArucoDetectionResult{" +
                "xCorners=" + Arrays.toString(xCorners) +
                ", yCorners=" + Arrays.toString(yCorners) +
                ", id=" + id +
                ", pose=" + pose +
                '}';
    }
}

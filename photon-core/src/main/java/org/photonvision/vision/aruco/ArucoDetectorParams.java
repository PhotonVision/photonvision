package org.photonvision.vision.aruco;

import edu.wpi.first.math.geometry.Pose3d;
import org.bytedeco.opencv.opencv_aruco.DetectorParameters;
import org.opencv.aruco.Aruco;
import static org.bytedeco.opencv.opencv_aruco.DetectorParameters.*;
import org.photonvision.vision.apriltag.AprilTagFamily;

public class ArucoDetectorParams {

   public ArucoDetectorParams(double decimate, int cornerIterations, boolean useAruco3) {
       DetectorParameters parameters = DetectorParameters.create();
       parameters.aprilTagQuadDecimate((float) decimate);
       parameters.cornerRefinementMethod(Aruco.CORNER_REFINE_APRILTAG);
       parameters.cornerRefinementMaxIterations(cornerIterations);
       parameters.useAruco3Detection(useAruco3);
   }
}

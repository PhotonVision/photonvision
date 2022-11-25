package org.photonvision.vision.aruco;

import edu.wpi.first.math.geometry.Pose3d;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Aruco;
import static org.bytedeco.opencv.opencv_aruco.DetectorParameters.*;
import org.photonvision.vision.apriltag.AprilTagFamily;

public class ArucoDetectorParams {

   public ArucoDetectorParams(double decimate, int cornerIterations, boolean useAruco3) {
       DetectorParameters parameters = DetectorParameters.create();
       parameters.set_aprilTagQuadDecimate((float) decimate);
       parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_APRILTAG);
       parameters.set_cornerRefinementMaxIterations(cornerIterations);
       parameters.set_useAruco3Detection(useAruco3);
   }
}

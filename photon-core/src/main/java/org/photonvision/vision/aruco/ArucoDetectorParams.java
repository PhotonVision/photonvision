package org.photonvision.vision.aruco;

import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Aruco;

public class ArucoDetectorParams {
   private ArucoDetectorParams() {

   }

   public static DetectorParameters getDetectorParams(double decimate, int cornerIterations, boolean useAruco3) {
       DetectorParameters parameters = DetectorParameters.create();
       parameters.set_aprilTagQuadDecimate((float) decimate);
       parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_APRILTAG);
       parameters.set_cornerRefinementMaxIterations(cornerIterations);
       parameters.set_useAruco3Detection(useAruco3);
       return parameters;
   }


}

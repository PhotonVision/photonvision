package org.photonvision.vision.aruco;

import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Aruco;

public class ArucoDetectorParams {
   private ArucoDetectorParams() {

   }

   public static DetectorParameters getDetectorParams(double decimate, int cornerIterations, boolean useAruco3) {
       DetectorParameters parameters = DetectorParameters.create();

      // parameters.set_aprilTagQuadDecimate(1);
       parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);
       parameters.set_cornerRefinementMaxIterations(200);
       //parameters.set_cornerRefinementWinSize();
       parameters.set_cornerRefinementMinAccuracy(.025);
       parameters.set_useAruco3Detection(true);
       return parameters;
   }


}

package org.photonvision.vision.aruco;

import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Aruco;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ArucoDetectorParams {
    private static final Logger logger = new Logger(ArucoDetector.class, LogGroup.VisionModule);
   private ArucoDetectorParams() {

   }

   public static DetectorParameters getDetectorParams(DetectorParameters curr, double decimate, int cornerIterations, double minAccuracy, boolean useAruco3) {
       if(curr == null || !(curr.get_aprilTagQuadDecimate() == decimate && curr.get_cornerRefinementMaxIterations() == cornerIterations && curr.get_useAruco3Detection() == useAruco3 && minAccuracy == curr.get_cornerRefinementMinAccuracy())) {
            DetectorParameters parameters = DetectorParameters.create();

            parameters.set_aprilTagQuadDecimate((float)decimate);
            parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);
             if(cornerIterations != 0) {
                 parameters.set_cornerRefinementMaxIterations(cornerIterations); // 200
             }
             if(minAccuracy != 0) {
                 parameters.set_cornerRefinementMinAccuracy(minAccuracy / 1000.0); // divides by 1000 because the UI multiplies it by 1000
             }

             parameters.set_useAruco3Detection(useAruco3);
           return parameters;
       }
       return  curr;

   }
}

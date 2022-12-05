package org.photonvision.vision.aruco;

import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Aruco;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ArucoDetectorParams {
    private static final Logger logger = new Logger(ArucoDetector.class, LogGroup.VisionModule);
   private ArucoDetectorParams() {

   }

   public static DetectorParameters getDetectorParams(DetectorParameters curr, float decimate, int cornerIterations, float minAccuracy, boolean useAruco3) {
       if(curr == null || !(curr.get_aprilTagQuadDecimate() == decimate && curr.get_cornerRefinementMaxIterations() == cornerIterations && curr.get_useAruco3Detection() == useAruco3 && minAccuracy == curr.get_cornerRefinementMinAccuracy())) {
            DetectorParameters parameters = DetectorParameters.create();

            parameters.set_aprilTagQuadDecimate(decimate);
            parameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);
             parameters.set_cornerRefinementMaxIterations(cornerIterations); // 200
             parameters.set_cornerRefinementMinAccuracy(minAccuracy); // .0025
             parameters.set_useAruco3Detection(useAruco3);
             logger.info(String.valueOf(parameters.get_useAruco3Detection()));
           return parameters;
       }
       return  curr;

   }
}

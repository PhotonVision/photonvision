/*
Copyright (c) 2022 Photon Vision. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of FIRST, WPILib, nor the names of other WPILib
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY FIRST AND OTHER WPILIB CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FIRST OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.photonvision.vision.aruco;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.apriltag.AprilTagDetectorParams;
import org.photonvision.vision.apriltag.AprilTagJNI;
import org.photonvision.vision.apriltag.DetectionResult;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

import java.util.ArrayList;

public class ArucoDetector {
    private static final Logger logger = new Logger(ArucoDetector.class, LogGroup.VisionModule);

    public ArucoDetectionResult[] detect(Mat grayscaleImg, CameraCalibrationCoefficients coeffs) {

        var cx = 0.0;
        var cy = 0.0;
        var fx = 0.0;
        var fy = 0.0;
        boolean calibrationSet = false;
        ArrayList<Mat> corners = new ArrayList();
        Pose3d tagPose = new Pose3d();
        Mat ids = new Mat();
        ArucoDetectionResult[] toReturn = new ArucoDetectionResult[corners.size()];
        var tvec = new Mat(3, 1, CvType.CV_64FC1);
        var rvec = new Mat(3,1,CvType.CV_64FC1);
        Aruco.detectMarkers(grayscaleImg, Dictionary.get(Aruco.DICT_APRILTAG_16h5), corners, ids);

        if (coeffs != null) {
            final Mat cameraMatrix = coeffs.getCameraIntrinsicsMat();
            if (cameraMatrix != null) {
                // Camera calibration has been done, we should be able to do pose estimation
                cx = cameraMatrix.get(0, 2)[0];
                cy = cameraMatrix.get(1, 2)[0];
                fx = cameraMatrix.get(0, 0)[0];
                fy = cameraMatrix.get(1, 1)[0];
                calibrationSet = true;
            }
        }

        for (int i = 0; i < corners.size(); i++) {
            Mat cornerMat = corners.get(i);
            if(calibrationSet) {
                Aruco.estimatePoseSingleMarkers(corners, (float) 0.1524, coeffs.getCameraIntrinsicsMat(), coeffs.getCameraExtrinsicsMat(), tvec, rvec);
                Translation3d translation =
                        new Translation3d(tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]);
                Rotation3d rotation =
                        new Rotation3d(
                                VecBuilder.fill(rvec.get(0, 0)[0], rvec.get(1, 0)[0], rvec.get(2, 0)[0]),
                                Core.norm(rvec));
                tagPose = MathUtils.convertOpenCVtoPhotonPose(new Transform3d(translation, rotation));
            }
            toReturn[i] = new ArucoDetectionResult(
                    new double[]{cornerMat.get(0, 0)[0], cornerMat.get(1, 0)[0], cornerMat.get(2, 0)[0], cornerMat.get(3, 0)[0]},
                    new double[]{cornerMat.get(0, 1)[0], cornerMat.get(1, 1)[0], cornerMat.get(2, 1)[0], cornerMat.get(3, 1)[0]},
                    (int) ids.get(i, 0)[0],
                    tagPose);


        }
        return toReturn;
    }

}

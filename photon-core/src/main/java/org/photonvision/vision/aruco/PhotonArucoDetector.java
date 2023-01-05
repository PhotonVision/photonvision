/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.aruco;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.ArucoDetector;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

public class PhotonArucoDetector {
    private static final Logger logger = new Logger(PhotonArucoDetector.class, LogGroup.VisionModule);

    Mat ids;

    Mat tvecs;
    Mat rvecs;
    ArrayList<Mat> corners;

    Mat cornerMat;
    Translation3d translation;
    Rotation3d rotation;
    double timeStartDetect;
    double timeEndDetect;
    Pose3d tagPose;
    double timeStartProcess;
    double timeEndProcess;
    double[] xCorners = new double[4];
    double[] yCorners = new double[4];

    public PhotonArucoDetector() {
        logger.debug("New Aruco Detector");
        ids = new Mat();
        tvecs = new Mat();
        rvecs = new Mat();
        corners = new ArrayList<Mat>();
        tagPose = new Pose3d();
        translation = new Translation3d();
        rotation = new Rotation3d();
    }

    public ArucoDetectionResult[] detect(
            Mat grayscaleImg,
            float tagSize,
            CameraCalibrationCoefficients coeffs,
            ArucoDetector detector) {
        detector.detectMarkers(grayscaleImg, corners, ids);
        if (coeffs != null)
            Aruco.estimatePoseSingleMarkers(
                    corners,
                    tagSize,
                    coeffs.getCameraIntrinsicsMat(),
                    coeffs.getDistCoeffsMat(),
                    rvecs,
                    tvecs);

        ArucoDetectionResult[] toReturn = new ArucoDetectionResult[corners.size()];
        timeStartProcess = System.currentTimeMillis();
        for (int i = 0; i < corners.size(); i++) {
            cornerMat = corners.get(i);
            // logger.debug(cornerMat.dump());
            xCorners =
                    new double[] {
                        cornerMat.get(0, 0)[0],
                        cornerMat.get(0, 1)[0],
                        cornerMat.get(0, 2)[0],
                        cornerMat.get(0, 3)[0]
                    };
            yCorners =
                    new double[] {
                        cornerMat.get(0, 0)[1],
                        cornerMat.get(0, 1)[1],
                        cornerMat.get(0, 2)[1],
                        cornerMat.get(0, 3)[1]
                    };
            cornerMat.release();
            // todo: only do pose est when 3d is enabled
            if (coeffs != null && xCorners[0] != 0) {
                final var axis =
                        VecBuilder.fill(rvecs.get(i, 0)[0], rvecs.get(i, 0)[1], rvecs.get(i, 0)[2]);
                translation =
                        new Translation3d(tvecs.get(i, 0)[0], tvecs.get(i, 0)[1], tvecs.get(i, 0)[2]); // 201
                tagPose =
                        MathUtils.convertOpenCVtoPhotonPose(
                                new Transform3d(translation, new Rotation3d(axis, axis.normF())));
            }

            toReturn[i] = new ArucoDetectionResult(xCorners, yCorners, (int) ids.get(i, 0)[0], tagPose);
        }
        rvecs.release();
        tvecs.release();
        ids.release();

        return toReturn;
    }
}

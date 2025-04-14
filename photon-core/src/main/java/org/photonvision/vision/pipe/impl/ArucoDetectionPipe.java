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

package org.photonvision.vision.pipe.impl;

import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.aruco.PhotonArucoDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class ArucoDetectionPipe
        extends CVPipe<CVMat, List<ArucoDetectionResult>, ArucoDetectionPipeParams>
        implements Releasable {
    // ArucoDetector wrapper class
    private final PhotonArucoDetector photonDetector = new PhotonArucoDetector();

    // Ratio multiplied with image size and added to refinement window size
    private static final double kRefineWindowImageRatio = 0.004;
    // Ratio multiplied with max marker diagonal length and added to refinement window size
    private static final double kRefineWindowMarkerRatio = 0.03;

    @Override
    protected List<ArucoDetectionResult> process(CVMat in) {
        var imgMat = in.getMat();

        // Sanity check -- image should not be empty
        if (imgMat.empty()) {
            // give up is best we can do here
            return List.of();
        }

        var detections = photonDetector.detect(imgMat);
        // manually do corner refinement ourselves
        if (params.useCornerRefinement) {
            for (var detection : detections) {
                double[] xCorners = detection.getXCorners();
                double[] yCorners = detection.getYCorners();
                Point[] cornerPoints =
                        new Point[] {
                            new Point(xCorners[0], yCorners[0]),
                            new Point(xCorners[1], yCorners[1]),
                            new Point(xCorners[2], yCorners[2]),
                            new Point(xCorners[3], yCorners[3])
                        };
                double bltr =
                        Math.hypot(
                                cornerPoints[2].x - cornerPoints[0].x, cornerPoints[2].y - cornerPoints[0].y);
                double brtl =
                        Math.hypot(
                                cornerPoints[3].x - cornerPoints[1].x, cornerPoints[3].y - cornerPoints[1].y);
                double minDiag = Math.min(bltr, brtl);
                int halfWindowLength =
                        (int) Math.ceil(kRefineWindowImageRatio * Math.min(imgMat.rows(), imgMat.cols()));
                halfWindowLength += (int) (minDiag * kRefineWindowMarkerRatio);
                // dont do refinement on small markers
                if (halfWindowLength < 4) continue;
                var halfWindowSize = new Size(halfWindowLength, halfWindowLength);
                var ptsMat = new MatOfPoint2f(cornerPoints);
                var criteria =
                        new TermCriteria(3, params.refinementMaxIterations, params.refinementMinErrorPx);
                Imgproc.cornerSubPix(imgMat, ptsMat, halfWindowSize, new Size(-1, -1), criteria);
                cornerPoints = ptsMat.toArray();
                for (int i = 0; i < cornerPoints.length; i++) {
                    var pt = cornerPoints[i];
                    xCorners[i] = pt.x;
                    yCorners[i] = pt.y;
                    // If we want to debug the refinement window, draw a rectangle on the image
                    if (params.debugRefineWindow) {
                        drawCornerRefineWindow(imgMat, pt, halfWindowLength);
                    }
                }
            }
        }
        return List.of(detections);
    }

    @Override
    public void setParams(ArucoDetectionPipeParams newParams) {
        if (this.params == null || !this.params.equals(newParams)) {
            System.out.println("Changing tag family to " + newParams.tagFamily);
            photonDetector
                    .getDetector()
                    .setDictionary(Objdetect.getPredefinedDictionary(newParams.tagFamily));
            var detectParams = photonDetector.getParams();

            detectParams.set_adaptiveThreshWinSizeMin(newParams.threshMinSize);
            detectParams.set_adaptiveThreshWinSizeStep(newParams.threshStepSize);
            detectParams.set_adaptiveThreshWinSizeMax(newParams.threshMaxSize);
            detectParams.set_adaptiveThreshConstant(newParams.threshConstant);

            detectParams.set_errorCorrectionRate(newParams.errorCorrectionRate);

            detectParams.set_useAruco3Detection(newParams.useAruco3);
            detectParams.set_minSideLengthCanonicalImg(newParams.aruco3MinCanonicalImgSide);
            detectParams.set_minMarkerLengthRatioOriginalImg((float) newParams.aruco3MinMarkerSideRatio);

            photonDetector.setParams(detectParams);
        }

        super.setParams(newParams);
    }

    public PhotonArucoDetector getPhotonDetector() {
        return photonDetector;
    }

    private void drawCornerRefineWindow(Mat outputMat, Point corner, int windowSize) {
        int thickness = (int) (Math.ceil(Math.max(outputMat.cols(), outputMat.rows()) * 0.003));
        var pt1 = new Point(corner.x - windowSize, corner.y - windowSize);
        var pt2 = new Point(corner.x + windowSize, corner.y + windowSize);
        Imgproc.rectangle(outputMat, pt1, pt2, new Scalar(0, 0, 255), thickness);
    }

    @Override
    public void release() {
        photonDetector.release();
    }
}

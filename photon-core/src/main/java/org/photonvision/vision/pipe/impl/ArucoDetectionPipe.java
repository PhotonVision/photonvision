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

    @Override
    protected List<ArucoDetectionResult> process(CVMat in) {
        var imgMat = in.getMat();

        // Sanity check -- image should not be empty
        if (imgMat.empty()) {
            // give up is best we can do here
            return List.of();
        }

        var detections = photonDetector.detect(imgMat);
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

            detectParams.set_cornerRefinementMethod(
                    newParams.useCornerRefinement
                            ? Objdetect.CORNER_REFINE_SUBPIX
                            : Objdetect.CORNER_REFINE_NONE);

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

    @Override
    public void release() {
        photonDetector.release();
    }
}

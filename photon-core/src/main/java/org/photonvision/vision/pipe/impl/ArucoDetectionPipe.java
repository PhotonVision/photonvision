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
import org.photonvision.jni.ArucoNanoDetector;
import org.photonvision.jni.ArucoNanoDetector.DetectionResult;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class ArucoDetectionPipe
        extends CVPipe<CVMat, List<ArucoDetectionResult>, ArucoDetectionPipe.ArucoDetectionPipeParams>
        implements Releasable {
    @Override
    protected List<ArucoDetectionResult> process(CVMat in) {
        var imgMat = in.getMat();

        // Sanity check -- image should not be empty
        if (imgMat.empty()) {
            // give up is best we can do here
            return List.of();
        }
        return detect(imgMat);
    }

    @Override
    public void setParams(ArucoDetectionPipeParams newParams) {
        super.setParams(newParams);
    }

    public static List<ArucoDetectionResult> detect(Mat in) {
        DetectionResult[] ret = ArucoNanoDetector.detect(in);

        return List.of(ret).stream()
                .map(it -> new ArucoDetectionResult(it.xCorners(), it.yCorners(), it.id()))
                .toList();
    }

    @Override
    public void release() {}

    public static class ArucoDetectionPipeParams {}
}

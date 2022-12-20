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

import edu.wpi.first.apriltag.jni.DetectionResult;
import java.util.List;
import org.photonvision.vision.apriltag.AprilTagDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionPipe
        extends CVPipe<CVMat, List<DetectionResult>, AprilTagDetectionPipeParams> {
    private final AprilTagDetector m_detector = new AprilTagDetector();

    boolean useNativePoseEst;

    @Override
    protected List<DetectionResult> process(CVMat in) {
        var ret =
                m_detector.detect(
                        in.getMat(),
                        params.cameraCalibrationCoefficients,
                        useNativePoseEst,
                        params.numIterations,
                        params.tagWidthMeters);
        if (ret == null) return List.of();
        return List.of(ret);
    }

    @Override
    public void setParams(AprilTagDetectionPipeParams params) {
        super.setParams(params);
        m_detector.updateParams(params.detectorParams);
    }

    public void setNativePoseEstimationEnabled(boolean enabled) {
        this.useNativePoseEst = enabled;
    }
}

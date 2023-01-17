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

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import java.util.List;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionPipe
        extends CVPipe<CVMat, List<AprilTagDetection>, AprilTagDetectionPipeParams> {
    private final AprilTagDetector m_detector = new AprilTagDetector();

    boolean useNativePoseEst;

    public AprilTagDetectionPipe() {
        super();

        m_detector.addFamily("tag16h5");
        m_detector.addFamily("tag36h11");
    }

    @Override
    protected List<AprilTagDetection> process(CVMat in) {
        if (in.getMat().empty()) {
            return List.of();
        }

        var ret = m_detector.detect(in.getMat());

        if (ret == null) {
            return List.of();
        }

        return List.of(ret);
    }

    @Override
    public void setParams(AprilTagDetectionPipeParams newParams) {
        if (this.params == null || !this.params.equals(newParams)) {
            m_detector.setConfig(newParams.detectorParams);

            m_detector.clearFamilies();
            m_detector.addFamily(newParams.family.getNativeName());
        }

        super.setParams(newParams);
    }

    public void setNativePoseEstimationEnabled(boolean enabled) {
        this.useNativePoseEst = enabled;
    }
}

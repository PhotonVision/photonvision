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
import org.photonvision.vision.apriltag.AprilTagJNI;
import org.photonvision.vision.apriltag.DetectionResult;
//import apriltag.TagDetection //
import org.photonvision.vision.pipe.CVPipe;


public class AprilTagDetectionPipe
        extends CVPipe<Mat, List<DetectionResult>, AprilTagDetectionPipe.AprilTagDetectionParams> {
    private long m_detector_ptr = 0L;

    @Override
    protected List<DetectionResult> process(Mat in) {
        return List.of(AprilTagJNI.AprilTag_Detect(m_detector_ptr, in));
    }

    @Override
    public void setParams(AprilTagDetectionParams params) {
        if(this.params == null) {
            super.setParams(params);
            createDetector(params.tagFamily);
        }
        else {
            if(!(this
            .params
            .tagFamily
            .equals(
                params
                .tagFamily))){
            createDetector(params.tagFamily);
        }
        super.setParams(params);
        }

    }

    private void createDetector(String fam) {
        if(m_detector_ptr != 0L) {
            //AprilTagJNI.AprilTag_Destroy(m_detector_ptr);
        }
        m_detector_ptr = AprilTagJNI.AprilTag_Create(fam, 1.0, 0.0, 4, false, true);
    }

    public static class AprilTagDetectionParams {
        private final String tagFamily;

        public AprilTagDetectionParams(String fam) {
            tagFamily = fam;
        }

        public String getFamily() {
            return tagFamily;
        }
    }
}

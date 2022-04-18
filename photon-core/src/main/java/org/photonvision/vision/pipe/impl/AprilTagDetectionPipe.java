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
import java.util.Objects;

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
        if(m_detector_ptr != 0L) {
            return List.of(AprilTagJNI.AprilTag_Detect(m_detector_ptr, in));
        }
        else {
            return List.of();
        }
        
    }

    @Override
    public void setParams(AprilTagDetectionParams params) {
        if(!params.equals(this.params)) {
            createDetector(params.tagFamily, params.decimate, params.blur, params.threads, params.debug, params.refineEdges);
        }
        super.setParams(params);
    }

    private void createDetector(String fam, double decimate, double blur, int threads, boolean debug, boolean refineEdges) {
        if(m_detector_ptr != 0L) {
            //AprilTagJNI.AprilTag_Destroy(m_detector_ptr);
        }
        m_detector_ptr = AprilTagJNI.AprilTag_Create(fam, decimate,blur, threads, debug, refineEdges);
    }

    public static class AprilTagDetectionParams {
        private final String tagFamily;
        private final double decimate;


        private final double blur;
        private final int threads;
        private final boolean debug;
        private final boolean refineEdges;

        public AprilTagDetectionParams(String tagFamily, double decimate, double blur, int threads, boolean debug,
                boolean refineEdges) {
            this.tagFamily = tagFamily;
            this.decimate = decimate;
            this.blur = blur;
            this.threads = threads;
            this.debug = debug;
            this.refineEdges = refineEdges;
        }

        public String getTagFamily() {
            return tagFamily;
        }

        public double getDecimate() {
            return decimate;
        }

        public double getBlur() {
            return blur;
        }

        public int getThreads() {
            return threads;
        }

        public boolean isDebug() {
            return debug;
        }

        public boolean isRefineEdges() {
            return refineEdges;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AprilTagDetectionParams that = (AprilTagDetectionParams) o;
            return Objects.equals(tagFamily, that.tagFamily)
                && Double.compare(decimate, that.decimate) == 0
                && Double.compare(blur, that.blur) == 0
                && threads == that.threads
                && debug == that.debug
                && refineEdges == that.refineEdges;
        }
    
    }
}

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
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagDetector;

public class AprilTagDetectionPipe
        extends CVPipe<
                CVMat, List<AprilTagDetection>, AprilTagDetectionPipe.AprilTagDetectionPipeParams>
        implements Releasable {
    /**
     * Smallest image, measured after decimation, that the native detector can be handed. apriltag's
     * gradient_clusters() reads out of bounds -- a SIGSEGV, not an exception -- once a decimated
     * dimension is down to two pixels, which a small enough static crop can produce. Nothing is
     * detectable at that size anyway, so we skip the detector rather than feed it such an image.
     */
    private static final int MIN_DECIMATED_DIMENSION = 4;

>>>>>>> 68555288 (slop from the slop machine)
    private AprilTagDetector m_detector = new AprilTagDetector();

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

        if (m_detector == null) {
            throw new RuntimeException("Apriltag detector was released!");
        }

        if (tooSmallToDetect(in.getMat(), params.detectorParams().quadDecimate)) {
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
            m_detector.setConfig(newParams.detectorParams());
            m_detector.setQuadThresholdParameters(newParams.quadParams());

            m_detector.clearFamilies();
            m_detector.addFamily(newParams.family().getNativeName());
        }

        super.setParams(newParams);
    }

    @Override
    public void release() {
        m_detector.close();
        m_detector = null;
    }

    /** Whether an image is too small for the native detector to safely handle. */
    private static boolean tooSmallToDetect(Mat image, float quadDecimate) {
        // Decimation below 1 doesn't shrink the image, and a zero factor would divide by zero.
        double factor = Math.max(1.0, quadDecimate);

        return decimatedDimension(image.cols(), factor) < MIN_DECIMATED_DIMENSION
                || decimatedDimension(image.rows(), factor) < MIN_DECIMATED_DIMENSION;
    }

    /** The size of one dimension of the decimated image, matching apriltag's own arithmetic. */
    private static int decimatedDimension(int dimension, double factor) {
        return 1 + (int) ((dimension - 1) / factor);
    }

    public static record AprilTagDetectionPipeParams(
            AprilTagFamily family,
            AprilTagDetector.Config detectorParams,
            AprilTagDetector.QuadThresholdParameters quadParams) {}
}

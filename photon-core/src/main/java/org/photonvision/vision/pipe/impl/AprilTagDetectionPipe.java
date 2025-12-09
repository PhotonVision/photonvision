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
import org.photonvision.vision.apriltag.AprilTagDetectorAdapter;
import org.photonvision.vision.apriltag.AprilTagDetectorBackend;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.apriltag.OptimizedAprilTagDetectorAdapter;
import org.photonvision.vision.apriltag.WpilibAprilTagDetectorAdapter;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionPipe
        extends CVPipe<
                CVMat, List<AprilTagDetection>, AprilTagDetectionPipe.AprilTagDetectionPipeParams>
        implements Releasable {
    private AprilTagDetectorAdapter detectorAdapter;

    public AprilTagDetectionPipe() {
        super();
    }

    @Override
    protected List<AprilTagDetection> process(CVMat in) {
        if (in.getMat().empty()) {
            return List.of();
        }

        if (detectorAdapter == null) {
            throw new RuntimeException("Apriltag detector backend not initialized!");
        }

        return detectorAdapter.detect(in);
    }

    @Override
    public void setParams(AprilTagDetectionPipeParams newParams) {
        if (detectorAdapter == null) {
            detectorAdapter = createAdapter(newParams.backend());
        } else if (detectorAdapter.getBackendType() != newParams.backend()) {
            detectorAdapter.release();
            detectorAdapter = createAdapter(newParams.backend());
        }

        detectorAdapter.setParams(newParams);
        super.setParams(newParams);
    }

    @Override
    public void release() {
        if (detectorAdapter != null) {
            detectorAdapter.release();
            detectorAdapter = null;
        }
    }

    private AprilTagDetectorAdapter createAdapter(AprilTagDetectorBackend backend) {
        return switch (backend) {
            case OPTIMIZED_UMICH -> new OptimizedAprilTagDetectorAdapter();
            case WPILIB -> new WpilibAprilTagDetectorAdapter();
        };
    }

    public static record AprilTagDetectionPipeParams(
            AprilTagFamily family,
            AprilTagDetector.Config detectorParams,
            AprilTagDetector.QuadThresholdParameters quadParams,
            AprilTagDetectorBackend backend) {}
}

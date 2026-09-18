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

package org.photonvision.vision.pipeline;

import java.util.List;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe.AprilTagDetectionPipeParams;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagDetector;

/** The CPU (libapriltag, via WPILib's AprilTagDetector JNI) AprilTag detection pipeline. */
public class AprilTagPipeline extends AbstractAprilTagPipeline<AprilTagPipelineSettings> {
    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();

    public AprilTagPipeline() {
        super();
        settings = new AprilTagPipelineSettings();
    }

    public AprilTagPipeline(AprilTagPipelineSettings settings) {
        super();
        this.settings = settings;
    }

    @Override
    protected void setDetectorParams(AprilTagPipelineSettings settings) {
        // Sanitize thread count - not supported to have fewer than 1 threads
        settings.threads = Math.max(1, settings.threads);

        var config = new AprilTagDetector.Config();
        config.numThreads = settings.threads;
        config.refineEdges = settings.refineEdges;
        config.quadSigma = (float) settings.blur;
        config.quadDecimate = settings.decimate;

        var quadParams = new AprilTagDetector.QuadThresholdParameters();
        // 5 was the default minClusterPixels in WPILib prior to 2025
        // increasing it causes detection problems when decimate > 1
        quadParams.minClusterPixels = 5;
        // these are the same as the values in WPILib 2025
        // setting them here to prevent upstream changes from changing behavior of the detector
        quadParams.maxNumMaxima = 10;
        quadParams.criticalAngle = 45 * Math.PI / 180.0;
        quadParams.maxLineFitMSE = 10.0f;
        quadParams.minWhiteBlackDiff = 5;
        quadParams.deglitch = false;

        aprilTagDetectionPipe.setParams(
                new AprilTagDetectionPipeParams(settings.tagFamily, config, quadParams));
    }

    @Override
    protected CVPipeResult<List<AprilTagDetection>> runDetection(Frame frame) {
        return aprilTagDetectionPipe.run(frame.processedImage);
    }

    @Override
    public void release() {
        aprilTagDetectionPipe.release();
        super.release();
    }
}

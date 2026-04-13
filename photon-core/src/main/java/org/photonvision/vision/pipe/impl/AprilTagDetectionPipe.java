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
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.apriltag.AprilTagBackendManager;
import org.photonvision.vision.apriltag.AprilTagDetectorBackend;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.apriltag.NvidiaAprilTagDetector;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionPipe
        extends CVPipe<
                Frame, List<AprilTagDetection>, AprilTagDetectionPipe.AprilTagDetectionPipeParams>
        implements Releasable {
    private static final Logger logger = new Logger(AprilTagDetectionPipe.class, LogGroup.VisionModule);

    private AprilTagDetector m_detector = new AprilTagDetector();
    private final NvidiaAprilTagDetector nvidiaDetector = new NvidiaAprilTagDetector();
    private final Mat cpuFallbackGray = new Mat();
    private AprilTagDetectorBackend lastDetectionBackend = AprilTagDetectorBackend.CPU_WPILIB;

    public AprilTagDetectionPipe() {
        super();

        m_detector.addFamily("tag16h5");
        m_detector.addFamily("tag36h11");
    }

    @Override
    protected List<AprilTagDetection> process(Frame frame) {
        if (frame.processedImage.getMat().empty() && frame.colorImage.getMat().empty()) {
            return List.of();
        }

        if (m_detector == null) {
            throw new RuntimeException("Apriltag detector was released!");
        }

        if (params.backend() == AprilTagDetectorBackend.NVIDIA_CUDA
                && !frame.colorImage.getMat().empty()) {
            try {
                var detections =
                        nvidiaDetector.detect(
                                frame.colorImage.getMat(), params.detectorParams().quadDecimate);
                lastDetectionBackend = AprilTagDetectorBackend.NVIDIA_CUDA;
                return detections;
            } catch (RuntimeException ex) {
                logger.warn(
                        "NVIDIA AprilTag detection failed; falling back to the WPILib CPU backend: "
                                + ex.getMessage());
                AprilTagBackendManager.markRuntimeFailure(
                        "the NVIDIA detector failed at runtime");
            }
        }

        var ret = m_detector.detect(getCpuInputMat(frame));
        lastDetectionBackend = AprilTagDetectorBackend.CPU_WPILIB;

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
        nvidiaDetector.release();
        cpuFallbackGray.release();
    }

    public AprilTagDetectorBackend getLastDetectionBackend() {
        return lastDetectionBackend;
    }

    private Mat getCpuInputMat(Frame frame) {
        if (!frame.processedImage.getMat().empty()) {
            return frame.processedImage.getMat();
        }

        if (frame.colorImage.getMat().empty()) {
            return cpuFallbackGray;
        }

        if (frame.colorImage.getMat().channels() == 1) {
            frame.colorImage.getMat().copyTo(cpuFallbackGray);
            return cpuFallbackGray;
        }

        if (frame.colorImage.getMat().channels() == 4) {
            Imgproc.cvtColor(frame.colorImage.getMat(), cpuFallbackGray, Imgproc.COLOR_BGRA2GRAY);
        } else {
            Imgproc.cvtColor(frame.colorImage.getMat(), cpuFallbackGray, Imgproc.COLOR_BGR2GRAY);
        }

        return cpuFallbackGray;
    }

    public static record AprilTagDetectionPipeParams(
            AprilTagFamily family,
            AprilTagDetector.Config detectorParams,
            AprilTagDetector.QuadThresholdParameters quadParams,
            AprilTagDetectorBackend backend) {}
}

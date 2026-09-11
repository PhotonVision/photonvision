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

import org.photonvision.vision.target.TargetModel;

/**
 * BETA. Settings for the Vulkan-accelerated (vkapriltag) AprilTag detector.
 *
 * <p>Deliberately does NOT have blur/refineEdges fields, unlike {@link AprilTagPipelineSettings}:
 * vkapriltag has no pre-blur stage, and edge refinement is explicitly out of scope for that
 * library (see vkapriltag's TagDecoder.h). Unlike those, {@link #decimation} IS present - as of
 * vkapriltag v1.3.0, decimation is a configurable {@code DetectorConfig} field rather than a fixed
 * 2x hardcoded into {@code GpuDetector} - so this is one knob this settings class does expose.
 */
public class VkAprilTagPipelineSettings extends AprilTagPipelineSettingsBase {
    /**
     * -1 selects vkapriltag's own scored auto-select (discrete &gt; integrated &gt; virtual &gt;
     * CPU, the last of which is never actually chosen - see VkAprilTagAvailability). Otherwise an
     * index from {@code VkAprilTagAvailability.getDevices()}.
     */
    public int vulkanDeviceIndex = -1;

    /**
     * Degree of parallelism for the CPU tail (per-blob quad fitting). 0 selects {@code
     * std::thread::hardware_concurrency()}.
     */
    public int cpuThreads = 0;

    /**
     * Integer downsampling factor applied by the Vulkan detector before thresholding/labelling. 1
     * disables decimation (full resolution), 2 is this pipeline's original fixed behavior (kept as
     * the default so existing saved pipelines behave identically after upgrading past vkapriltag
     * v1.3.0), 4 halves resolution again, etc. Must evenly divide the current camera mode's width
     * and height, or the pipeline falls back to CPU (see {@code VkAprilTagDetectionPipe}).
     */
    public int decimation = 2;

    /** Which implementation runs single-tag pose estimation for this pipeline. */
    public enum PoseEstimatorBackend {
        CPU,
        VULKAN
    }

    /**
     * CPU (WPILib's own pose estimator) is the well-tested default. VULKAN uses vkapriltag's native
     * PoseEstimator instead - measured much faster than unmodified libapriltag per tag, but that
     * comparison is NOT against WPILib's own estimator (what CPU actually runs), so the real-world
     * benefit for this pipeline is unverified; compare results carefully before relying on it.
     * Multi-tag pose estimation (MultiTargetPNPPipe) is unaffected either way - vkapriltag's
     * PoseEstimator has no multi-tag capability.
     */
    public PoseEstimatorBackend poseEstimatorBackend = PoseEstimatorBackend.CPU;

    public VkAprilTagPipelineSettings() {
        super();
        pipelineType = PipelineType.AprilTagVulkan;
        targetModel = TargetModel.kAprilTag6p5in_36h11;
        cameraExposureRaw = 20;
        cameraAutoExposure = false;
        ledMode = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + vulkanDeviceIndex;
        result = prime * result + cpuThreads;
        result = prime * result + decimation;
        result = prime * result + poseEstimatorBackend.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        VkAprilTagPipelineSettings other = (VkAprilTagPipelineSettings) obj;
        if (vulkanDeviceIndex != other.vulkanDeviceIndex) return false;
        if (cpuThreads != other.cpuThreads) return false;
        if (decimation != other.decimation) return false;
        if (poseEstimatorBackend != other.poseEstimatorBackend) return false;
        return true;
    }
}

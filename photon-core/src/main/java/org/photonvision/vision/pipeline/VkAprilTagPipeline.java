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
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe;
import org.photonvision.vision.pipe.impl.VkAprilTagDetectionPipe;
import org.photonvision.vision.pipe.impl.VkAprilTagDetectionPipe.VkAprilTagDetectionPipeParams;
import org.photonvision.vision.pipe.impl.VkAprilTagPoseEstimatorPipe;
import org.wpilib.vision.apriltag.AprilTagDetection;

/**
 * BETA. The Vulkan-accelerated (vkapriltag) AprilTag detection pipeline - a sibling to {@link
 * AprilTagPipeline}, not a toggle inside it, so it can be deleted in one commit if the "beta for a
 * year or two" window in the integration plan ends without this being kept up to date. Everything
 * downstream of detection (pose estimation, multi-target, target-list assembly, drawing, NT
 * publishing) is shared with the CPU pipeline via {@link AbstractAprilTagPipeline}, since both
 * produce the same {@code List<AprilTagDetection>}.
 *
 * <p>Falls back to CPU detection automatically - see {@link VkAprilTagDetectionPipe} - when Vulkan
 * is unavailable, the frame size isn't evenly divisible by {@code settings.decimation}, or native
 * detector creation fails. The pipeline stays {@code AprilTagVulkan}-typed while doing so; {@link
 * #isVulkanActive} reports which one actually ran, for the UI to surface.
 */
public class VkAprilTagPipeline extends AbstractAprilTagPipeline<VkAprilTagPipelineSettings> {
    private final VkAprilTagDetectionPipe detectionPipe = new VkAprilTagDetectionPipe();

    public VkAprilTagPipeline() {
        super();
        settings = new VkAprilTagPipelineSettings();
    }

    public VkAprilTagPipeline(VkAprilTagPipelineSettings settings) {
        super();
        this.settings = settings;
    }

    @Override
    protected void setDetectorParams(VkAprilTagPipelineSettings settings) {
        detectionPipe.setParams(
                new VkAprilTagDetectionPipeParams(
                        settings.tagFamily,
                        frameStaticProperties.imageWidth,
                        frameStaticProperties.imageHeight,
                        settings.decimation,
                        settings.vulkanDeviceIndex,
                        settings.cpuThreads));
    }

    @Override
    protected void configureSingleTagPoseEstimatorBackend() {
        boolean wantVulkan =
                settings.poseEstimatorBackend == VkAprilTagPipelineSettings.PoseEstimatorBackend.VULKAN;
        boolean isVulkan = singleTagPoseEstimatorPipe instanceof VkAprilTagPoseEstimatorPipe;
        if (wantVulkan == isVulkan) return;

        singleTagPoseEstimatorPipe.release();
        singleTagPoseEstimatorPipe =
                wantVulkan ? new VkAprilTagPoseEstimatorPipe() : new AprilTagPoseEstimatorPipe();
    }

    @Override
    protected CVPipeResult<List<AprilTagDetection>> runDetection(Frame frame) {
        return detectionPipe.run(frame.processedImage);
    }

    /** True while the most recent frame's detections came off the GPU, not the CPU fallback. */
    public boolean isVulkanActive() {
        return detectionPipe.isVulkanActive();
    }

    @Override
    public void release() {
        detectionPipe.release();
        super.release();
    }
}

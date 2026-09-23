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

import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagPoseEstimate;

/**
 * The one swappable step {@link AbstractAprilTagPipeline} needs for single-tag pose estimation -
 * "takes an {@code AprilTagDetection}, returns an {@code AprilTagPoseEstimate}" - so a Vulkan-
 * native backend ({@link VkAprilTagPoseEstimatorPipe}) can substitute for the default WPILib/CPU
 * one ({@link AprilTagPoseEstimatorPipe}) without either the multi-tag PNP pipe or the {@code
 * doSingleTargetAlways} fallback-derivation logic needing to know which one ran.
 *
 * <p>Deliberately narrow: this only covers pose estimation, not detection (which already forked
 * into separate pipeline classes, {@link org.photonvision.vision.pipeline.AprilTagPipeline} vs.
 * {@link org.photonvision.vision.pipeline.VkAprilTagPipeline}) - not a speculative multi-purpose
 * "backend" abstraction.
 *
 * <p>{@code run}/{@code setParams}/{@code release} are declared with the exact signatures {@link
 * AprilTagPoseEstimatorPipe} (via its {@link org.photonvision.vision.pipe.CVPipe} superclass) and
 * {@link VkAprilTagPoseEstimatorPipe} already have, so both satisfy this interface without any
 * adapter code.
 */
public interface SingleTagPoseEstimator {
    CVPipeResult<AprilTagPoseEstimate> run(AprilTagDetection detection);

    void setParams(AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams params);

    void release();
}

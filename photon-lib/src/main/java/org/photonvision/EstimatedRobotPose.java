/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import java.util.List;
import java.util.stream.Collectors;

import org.photonvision.targeting.PhotonPipelineResult;

import edu.wpi.first.math.geometry.Pose3d;

public class EstimatedRobotPose {
    Pose3d estimatedPose;
    List<CameraPipelineResult> cameraPipelineResults;

    public EstimatedRobotPose(Pose3d estimatedPose, List<CameraPipelineResult> cameraPipelineResults) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = cameraPipelineResults;
    }

    public EstimatedRobotPose(Pose3d estimatedPose, CameraPipelineResult cameraPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(cameraPipelineResult);
    }

    public EstimatedRobotPose(Pose3d estimatedPose, PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(new CameraPipelineResult(camera, photonPipelineResult));
    }

    public double getTimestamp() {
        return cameraPipelineResults
                .stream()
                .flatMap(r -> r.photonPipelineResults.stream())
                .collect(Collectors.averagingDouble(cpr -> cpr.getTimestampSeconds()));
    }

    public static class CameraPipelineResult {
        PhotonCamera camera;
        List<PhotonPipelineResult> photonPipelineResults;

        public CameraPipelineResult(PhotonCamera camera, List<PhotonPipelineResult> photonPipelineResults) {
            this.camera = camera;
            this.photonPipelineResults = photonPipelineResults;
        }
        public CameraPipelineResult(PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
            this.camera = camera;
            this.photonPipelineResults = List.of(photonPipelineResult);
        }

    }
}

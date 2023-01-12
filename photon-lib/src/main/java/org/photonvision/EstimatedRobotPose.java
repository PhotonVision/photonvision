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

import edu.wpi.first.math.geometry.Pose3d;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.targeting.PhotonPipelineResult;

/** An estimated pose based on pipeline results */
public class EstimatedRobotPose {

    /** The estimated pose */
    public Pose3d estimatedPose;

    /** Camera and pipeline results used in the pose estimate */
    public List<CameraPipelineResult> cameraPipelineResults;

    /**
     * Constructs an EstimatedRobotPose
     *
     * @param estimatedPose estimated pose
     * @param cameraPipelineResults list of camera pipeline results
     */
    public EstimatedRobotPose(
            Pose3d estimatedPose, List<CameraPipelineResult> cameraPipelineResults) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = cameraPipelineResults;
    }

    /**
     * Constructs an EstimatedRobotPose
     *
     * @param estimatedPose estimated pose
     * @param cameraPipelineResult camera pipeline result
     */
    public EstimatedRobotPose(Pose3d estimatedPose, CameraPipelineResult cameraPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(cameraPipelineResult);
    }

    /**
     * Constructs an EstimatedRobotPose
     *
     * @param estimatedPose estimated pose
     * @param camera camera used in estimate
     * @param photonPipelineResult photon pipeline result used in estimate
     */
    public EstimatedRobotPose(
            Pose3d estimatedPose, PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(new CameraPipelineResult(camera, photonPipelineResult));
    }

    /**
     * Returns the timestamp for the pose estimate
     *
     * @return timestamp in seconds
     */
    public double getTimestamp() {
        return cameraPipelineResults.stream()
                .flatMap(r -> r.photonPipelineResults.stream())
                .collect(Collectors.averagingDouble(cpr -> cpr.getTimestampSeconds()));
    }

    /** A camera and list of photon pipeline results used to estimate the pose */
    public static class CameraPipelineResult {

        /** Camera used to estimate the pose */
        public PhotonCamera camera;

        /** Photon pipeline results used to estimate the pose */
        public List<PhotonPipelineResult> photonPipelineResults;

        /**
         * Constructs a CameraPipelineResult
         *
         * @param camera camera
         * @param photonPipelineResults photon pipeline results
         */
        public CameraPipelineResult(
                PhotonCamera camera, List<PhotonPipelineResult> photonPipelineResults) {
            this.camera = camera;
            this.photonPipelineResults = photonPipelineResults;
        }

        /**
         * Constructs a CameraPipelineResult
         *
         * @param camera camera
         * @param photonPipelineResult photon pipeline result
         */
        public CameraPipelineResult(PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
            this.camera = camera;
            this.photonPipelineResults = List.of(photonPipelineResult);
        }
    }
}

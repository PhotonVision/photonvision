/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.numbers.N5;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.Arrays;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

/** @deprecated Use {@link PhotonCameraSim} instead */
@Deprecated
@SuppressWarnings("unused")
public class SimPhotonCamera {
    NTTopicSet ts = new NTTopicSet();
    PhotonPipelineResult latestResult;
    private long heartbeatCounter = 0;

    /**
     * Constructs a Simulated PhotonCamera from a root table.
     *
     * @param instance The NetworkTableInstance to pull data from. This can be a custom instance in
     *     simulation, but should *usually* be the default NTInstance from
     *     NetworkTableInstance::getDefault
     * @param cameraName The name of the camera, as seen in the UI.
     */
    public SimPhotonCamera(NetworkTableInstance instance, String cameraName) {
        ts.removeEntries();
        ts.subTable = instance.getTable(PhotonCamera.kTableName).getSubTable(cameraName);
        ts.updateEntries();
    }

    /**
     * Publishes the camera intrinsics matrix. The matrix should be in the form: spotless:off
     * fx  0   cx
     * 0   fy  cy
     * 0   0   1
     * @param cameraMatrix The cam matrix
     * spotless:on
     */
    public void setCameraIntrinsicsMat(Matrix<N3, N3> cameraMatrix) {
        ts.cameraIntrinsicsPublisher.set(cameraMatrix.getData());
    }

    /**
     * Publishes the camera distortion matrix. The matrix should be in the form [k1 k2 p1 p2 k3]. See
     * more: https://docs.opencv.org/3.4/d4/d94/tutorial_camera_calibration.html
     *
     * @param distortionMat The distortion mat
     */
    public void setCameraDistortionMat(Matrix<N5, N1> distortionMat) {
        ts.cameraDistortionPublisher.set(distortionMat.getData());
    }

    /**
     * Constructs a Simulated PhotonCamera from the name of the camera.
     *
     * @param cameraName The nickname of the camera (found in the PhotonVision UI).
     */
    public SimPhotonCamera(String cameraName) {
        this(NetworkTableInstance.getDefault(), cameraName);
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT.
     *
     * @param latencyMillis Latency of the provided frame
     * @param targets Each target detected
     */
    public void submitProcessedFrame(double latencyMillis, PhotonTrackedTarget... targets) {
        submitProcessedFrame(latencyMillis, Arrays.asList(targets));
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT.
     *
     * @param latencyMillis Latency of the provided frame
     * @param sortMode Order in which to sort targets
     * @param targets Each target detected
     */
    public void submitProcessedFrame(
            double latencyMillis, PhotonTargetSortMode sortMode, PhotonTrackedTarget... targets) {
        submitProcessedFrame(latencyMillis, sortMode, Arrays.asList(targets));
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT.
     *
     * @param latencyMillis Latency of the provided frame
     * @param targetList List of targets detected
     */
    public void submitProcessedFrame(double latencyMillis, List<PhotonTrackedTarget> targetList) {
        submitProcessedFrame(latencyMillis, null, targetList);
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT.
     *
     * @param latencyMillis Latency of the provided frame
     * @param sortMode Order in which to sort targets
     * @param targetList List of targets detected
     */
    public void submitProcessedFrame(
            double latencyMillis, PhotonTargetSortMode sortMode, List<PhotonTrackedTarget> targetList) {
        ts.latencyMillisEntry.set(latencyMillis);

        if (sortMode != null) {
            targetList.sort(sortMode.getComparator());
        }

        PhotonPipelineResult newResult = new PhotonPipelineResult(latencyMillis, targetList);
        var newPacket = new Packet(newResult.getPacketSize());
        newResult.populatePacket(newPacket);
        ts.rawBytesEntry.set(newPacket.getData());

        boolean hasTargets = newResult.hasTargets();
        ts.hasTargetEntry.set(hasTargets);
        if (!hasTargets) {
            ts.targetPitchEntry.set(0.0);
            ts.targetYawEntry.set(0.0);
            ts.targetAreaEntry.set(0.0);
            ts.targetPoseEntry.set(new double[] {0.0, 0.0, 0.0});
            ts.targetSkewEntry.set(0.0);
        } else {
            var bestTarget = newResult.getBestTarget();

            ts.targetPitchEntry.set(bestTarget.getPitch());
            ts.targetYawEntry.set(bestTarget.getYaw());
            ts.targetAreaEntry.set(bestTarget.getArea());
            ts.targetSkewEntry.set(bestTarget.getSkew());

            var transform = bestTarget.getBestCameraToTarget();
            double[] poseData = {
                transform.getX(), transform.getY(), transform.getRotation().toRotation2d().getDegrees()
            };
            ts.targetPoseEntry.set(poseData);
        }

        ts.heartbeatPublisher.set(heartbeatCounter++);

        latestResult = newResult;
    }

    PhotonPipelineResult getLatestResult() {
        return latestResult;
    }
}

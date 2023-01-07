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

import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public class DynamicTargetPoseEstimator {
    private final Set<Integer> m_dynamicTargets;
    private final List<WrappedCamera> m_cameras;
    private final Map<Integer, Pose3d> m_targetPositions = new HashMap<>();

    public DynamicTargetPoseEstimator(
            Set<Integer> targetIds, List<Pair<PhotonCamera, Transform3d>> cameras) {
        m_cameras = WrappedCamera.createListFromMap(cameras);
        m_dynamicTargets = Collections.unmodifiableSet(targetIds);
    }

    public Map<Integer, Pose3d> update(Pose3d robotPose) {
        // Update camera results - we only want to do this once per update
        updateCameraResults();

        // Get result from strategy
        // TODO: add more strategies
        var result = defaultStrategy(robotPose);

        // Update target positions with results of strategy
        m_targetPositions.putAll(result);

        return result;
    }

    private Map<Integer, Pose3d> defaultStrategy(Pose3d robotPose) {
        Map<Integer, WrappedCamera> lowestAmbiguityCameras = new HashMap<>();
        Map<Integer, PhotonTrackedTarget> lowestAmbiguityTargets = new HashMap<>();
        Map<Integer, Double> lowestAmbiguityScores = new HashMap<>();

        for (WrappedCamera camera : m_cameras) {
            List<PhotonTrackedTarget> targets = camera.latestResult.getTargets();
            for (PhotonTrackedTarget target : targets) {
                int fiducialId = target.getFiducialId();
                if (!isDynamicTarget(fiducialId)) {
                    continue;
                }
                if (target.getPoseAmbiguity()
                        < lowestAmbiguityScores.getOrDefault(fiducialId, Double.MAX_VALUE)) {
                    lowestAmbiguityCameras.put(fiducialId, camera);
                    lowestAmbiguityTargets.put(fiducialId, target);
                    lowestAmbiguityScores.put(fiducialId, target.getPoseAmbiguity());
                }
            }
        }

        Map<Integer, Pose3d> results = new HashMap<>();

        for (int i : lowestAmbiguityScores.keySet()) {
            var pair = Pair.of(lowestAmbiguityCameras.get(i), lowestAmbiguityTargets.get(i));
            results.put(i, calculateTargetPose(robotPose, pair));
        }

        return results;
    }

    public Optional<Pose3d> getLastSeenTargetPose(int fiducialId) {
        return Optional.ofNullable(m_targetPositions.get(fiducialId));
    }

    private void updateCameraResults() {
        m_cameras.forEach(x -> x.update());
    }

    private boolean isDynamicTarget(int fiducialId) {
        return m_dynamicTargets.contains(fiducialId);
    }

    private Pose3d calculateTargetPose(
            Pose3d robotPose, Pair<WrappedCamera, PhotonTrackedTarget> target) {
        return robotPose
                .transformBy(target.getFirst().robotToCamera)
                .transformBy(target.getSecond().getBestCameraToTarget());
    }

    private static class WrappedCamera {
        private final PhotonCamera camera;
        private final Transform3d robotToCamera;
        private PhotonPipelineResult latestResult;

        public WrappedCamera(PhotonCamera camera, Transform3d robotToCamera) {
            this.camera = camera;
            this.robotToCamera = robotToCamera;
        }

        public void update() {
            latestResult = camera.getLatestResult();
        }

        public static List<WrappedCamera> createListFromMap(
                List<Pair<PhotonCamera, Transform3d>> cameras) {
            return cameras.stream().map(x -> new WrappedCamera(x.getFirst(), x.getSecond())).toList();
        }
    }
}

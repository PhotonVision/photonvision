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

package org.photonvision;

import org.photonvision.estimation.TargetModel;
import org.photonvision.jni.GTSAMConcurrentLocalizer;
import org.photonvision.jni.GTSAMExtras;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.wpilib.driverstation.DriverStationErrors;
import org.wpilib.fields.Field;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.linalg.Vector;
import org.wpilib.math.numbers.N5;
import org.wpilib.math.numbers.N6;
import org.wpilib.system.Notifier;

public class PhotonLocalizer {
    private GTSAMConcurrentLocalizer core;
    private GTSAMExtras.NoiseModel odomNoise;
    private Notifier notifier;

    public PhotonLocalizer(Field layout, TargetModel model) {
        core = new GTSAMConcurrentLocalizer(layout, model);
        odomNoise = GTSAMExtras.NoiseModel.Diagonal(VecBuilder.fill(0.1, 0.1, 0.1, 0.1, 0.1, 0.1));
        notifier =
                new Notifier(
                        () -> {
                            try {
                                core.step();
                            } catch (Exception e) {
                                DriverStationErrors.reportError(
                                        "[PhotonLocalizer] Exception in GTSAMRunner step: " + e.getMessage(), false);
                            }
                        });
    }

    public static final record GTSAMCamConfig(
            Transform3d robotToCamera, GTSAMExtras.Cal3S2 cameraCal, GTSAMExtras.NoiseModel pixelNoise) {
        public GTSAMCamConfig(Transform3d robotToCamera, Vector<N5> cameraCal, double sigma) {
            this(
                    robotToCamera,
                    GTSAMExtras.Cal3S2.FromVector(cameraCal),
                    GTSAMExtras.NoiseModel.Diagonal(VecBuilder.fill(sigma, sigma)));
        }

        public GTSAMCamConfig(
                Transform3d robotToCamera,
                GTSAMExtras.Cal3S2 cameraCal,
                GTSAMExtras.NoiseModel pixelNoise) {
            this.robotToCamera = robotToCamera;
            this.cameraCal = cameraCal;
            this.pixelNoise = pixelNoise;
        }
    }

    public static final record GTSAMPoseEstimate(Pose3d pose, double timestamp) {}

    public void start() {
        notifier.startPeriodic(0.02);
    }

    public void stop() {
        notifier.stop();
    }

    public void setOdomNoise(Vector<N6> noise) {
        odomNoise = GTSAMExtras.NoiseModel.Diagonal(noise);
    }

    public void submitOdometry(Pose3d odom, long timestamp) {
        core.submitOdometry(odom, odomNoise, timestamp);
    }

    public void submitVisionObservation(
            PhotonPipelineResult visionObservation, GTSAMCamConfig cameraConfig) {
        for (PhotonTrackedTarget target : visionObservation.getTargets()) {
            double[] corners = new double[8];
            for (int i = 0; i < 4; i++) {
                corners[i * 2] = target.getDetectedCorners().get(i).x;
                corners[i * 2 + 1] = target.getDetectedCorners().get(i).y;
            }
            core.submitTagObservation(
                    visionObservation.metadata.getCaptureTimestampNanos(),
                    target.getFiducialId(),
                    corners,
                    cameraConfig.cameraCal,
                    cameraConfig.robotToCamera,
                    cameraConfig.pixelNoise);
        }
    }

    public void submitReset(Pose3d pose, long timestamp) {
        core.submitReset(pose, odomNoise, timestamp);
    }

    public GTSAMPoseEstimate getLatestPoseEstimate() {
        Pose3d pose = core.getLatestWorldToBody();
        long timestamp = core.getLatestTimestamp();
        return new GTSAMPoseEstimate(pose, timestamp);
    }

    public Vector<N6> getPoseStdDevs() {
        return core.getPoseComponentStdDevs();
    }
}

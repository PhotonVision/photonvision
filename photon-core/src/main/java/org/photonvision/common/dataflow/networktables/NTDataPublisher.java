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

package org.photonvision.common.dataflow.networktables;

import com.ctre.phoenix6.hardware.Pigeon2;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTablesJNI;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final Logger logger = new Logger(NTDataPublisher.class, LogGroup.General);

    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;

    private final NTTopicSet ts = new NTTopicSet();
    private final NTTopicSet trig_ts = new NTTopicSet();

    private final Pigeon2 pidgey = new Pigeon2(10, "can0");
    private final PhotonPoseEstimator photonPoseEstimator =
            new PhotonPoseEstimator(
                    ConfigManager.getInstance().getConfig().getApriltagFieldLayout(),
                    PhotonPoseEstimator.PoseStrategy.PNP_DISTANCE_TRIG_SOLVE,
                    Transform3d.kZero);
    private final double nanosInSecond = 1e9;

    NTDataChangeListener pipelineIndexListener;
    private final Supplier<Integer> pipelineIndexSupplier;
    private final Consumer<Integer> pipelineIndexConsumer;

    NTDataChangeListener driverModeListener;
    private final BooleanSupplier driverModeSupplier;
    private final Consumer<Boolean> driverModeConsumer;

    public NTDataPublisher(
            String cameraNickname,
            Supplier<Integer> pipelineIndexSupplier,
            Consumer<Integer> pipelineIndexConsumer,
            BooleanSupplier driverModeSupplier,
            Consumer<Boolean> driverModeConsumer) {
        this.pipelineIndexSupplier = pipelineIndexSupplier;
        this.pipelineIndexConsumer = pipelineIndexConsumer;
        this.driverModeSupplier = driverModeSupplier;
        this.driverModeConsumer = driverModeConsumer;

        updateCameraNickname(cameraNickname);
        updateEntries();
    }

    private void onPipelineIndexChange(NetworkTableEvent entryNotification) {
        var newIndex = (int) entryNotification.valueData.value.getInteger();
        var originalIndex = pipelineIndexSupplier.get();

        // ignore indexes below 0
        if (newIndex < 0) {
            ts.pipelineIndexPublisher.set(originalIndex);
            trig_ts.pipelineIndexPublisher.set(originalIndex);
            return;
        }

        if (newIndex == originalIndex) {
            logger.debug("Pipeline index is already " + newIndex);
            return;
        }

        pipelineIndexConsumer.accept(newIndex);
        var setIndex = pipelineIndexSupplier.get();
        if (newIndex != setIndex) { // set failed
            ts.pipelineIndexPublisher.set(setIndex);
            trig_ts.pipelineIndexPublisher.set(setIndex);
            // TODO: Log
        }
        logger.debug("Set pipeline index to " + newIndex);
    }

    private void onDriverModeChange(NetworkTableEvent entryNotification) {
        var newDriverMode = entryNotification.valueData.value.getBoolean();
        var originalDriverMode = driverModeSupplier.getAsBoolean();

        if (newDriverMode == originalDriverMode) {
            logger.debug("Driver mode is already " + newDriverMode);
            return;
        }

        driverModeConsumer.accept(newDriverMode);
        logger.debug("Set driver mode to " + newDriverMode);
    }

    private void removeEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();
        ts.removeEntries();
        trig_ts.removeEntries();
    }

    private void updateEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();

        ts.updateEntries();
        trig_ts.updateEntries();

        pipelineIndexListener =
                new NTDataChangeListener(
                        ts.subTable.getInstance(), ts.pipelineIndexRequestSub, this::onPipelineIndexChange);

        driverModeListener =
                new NTDataChangeListener(
                        ts.subTable.getInstance(), ts.driverModeSubscriber, this::onDriverModeChange);
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        ts.subTable = rootTable.getSubTable(newCameraNickname);
        trig_ts.subTable = rootTable.getSubTable(newCameraNickname + "_trig");
        updateEntries();
    }

    @Override
    public void accept(CVPipelineResult result) {
        CVPipelineResult acceptedResult;
        if (result
                instanceof
                CalibrationPipelineResult) // If the data is from a calibration pipeline, override the list
            // of targets to be null to prevent the data from being sent and
            // continue to post blank/zero data to the network tables
            acceptedResult =
                    new CVPipelineResult(
                            result.sequenceID,
                            result.processingNanos,
                            result.fps,
                            List.of(),
                            result.inputAndOutputFrame);
        else acceptedResult = result;
        var now = NetworkTablesJNI.now();
        var captureMicros = MathUtils.nanosToMicros(result.getImageCaptureTimestampNanos());

        var offset = NetworkTablesManager.getInstance().getOffset();

        // Transform the metadata timestamps from the local nt::Now timebase to the Time Sync Server's
        // timebase
        var simplified =
                new PhotonPipelineResult(
                        acceptedResult.sequenceID,
                        captureMicros + offset,
                        now + offset,
                        NetworkTablesManager.getInstance().getTimeSinceLastPong(),
                        TrackedTarget.simpleFromTrackedTargets(acceptedResult.targets),
                        acceptedResult.multiTagResult);

        if (pidgey.isConnected()) {
            Rotation3d pidgeyHeading =
                    new Rotation3d(
                            pidgey.getRoll().getValue(),
                            pidgey.getPitch().getValue(),
                            pidgey.getYaw().getValue());
            photonPoseEstimator.addHeadingData(
                    ((double) result.getImageCaptureTimestampNanos() / nanosInSecond), pidgeyHeading);

            Optional<EstimatedRobotPose> estimatedPose = photonPoseEstimator.update(simplified);
            if (estimatedPose.isPresent()) {
                EstimatedRobotPose roboPose = estimatedPose.get();
                trig_ts.targetPoseEntry.set(
                        new Transform3d(
                                roboPose.estimatedPose.getTranslation(), roboPose.estimatedPose.getRotation()));
                trig_ts.targetPitchEntry.set(pidgey.getPitch().getValueAsDouble());
                trig_ts.targetYawEntry.set(pidgey.getYaw().getValueAsDouble());
            } else {
                ts.targetPitchEntry.set(0);
                ts.targetYawEntry.set(0);
                ts.targetAreaEntry.set(0);
                ts.targetSkewEntry.set(0);
                ts.targetPoseEntry.set(new Transform3d());
                ts.bestTargetPosX.set(0);
                ts.bestTargetPosY.set(0);
            }
        }

        // random guess at size of the array
        ts.resultPublisher.set(simplified, 1024);
        if (ConfigManager.getInstance().getConfig().getNetworkConfig().shouldPublishProto) {
            ts.protoResultPublisher.set(simplified);
        }

        ts.pipelineIndexPublisher.set(pipelineIndexSupplier.get());
        ts.driverModePublisher.set(driverModeSupplier.getAsBoolean());
        ts.latencyMillisEntry.set(acceptedResult.getLatencyMillis());
        ts.fpsEntry.set(acceptedResult.fps);
        ts.hasTargetEntry.set(acceptedResult.hasTargets());

        if (acceptedResult.hasTargets()) {
            var bestTarget = acceptedResult.targets.get(0);

            ts.targetPitchEntry.set(bestTarget.getPitch());
            ts.targetYawEntry.set(bestTarget.getYaw());
            ts.targetAreaEntry.set(bestTarget.getArea());
            ts.targetSkewEntry.set(bestTarget.getSkew());

            var pose = bestTarget.getBestCameraToTarget3d();
            ts.targetPoseEntry.set(pose);

            var targetOffsetPoint = bestTarget.getTargetOffsetPoint();
            ts.bestTargetPosX.set(targetOffsetPoint.x);
            ts.bestTargetPosY.set(targetOffsetPoint.y);
        } else {
            ts.targetPitchEntry.set(0);
            ts.targetYawEntry.set(0);
            ts.targetAreaEntry.set(0);
            ts.targetSkewEntry.set(0);
            ts.targetPoseEntry.set(new Transform3d());
            ts.bestTargetPosX.set(0);
            ts.bestTargetPosY.set(0);
        }

        // Something in the result can sometimes be null -- so check probably too many things
        if (acceptedResult.inputAndOutputFrame != null
                && acceptedResult.inputAndOutputFrame.frameStaticProperties != null
                && acceptedResult.inputAndOutputFrame.frameStaticProperties.cameraCalibration != null) {
            var fsp = acceptedResult.inputAndOutputFrame.frameStaticProperties;
            ts.cameraIntrinsicsPublisher.accept(fsp.cameraCalibration.getIntrinsicsArr());
            ts.cameraDistortionPublisher.accept(fsp.cameraCalibration.getDistCoeffsArr());
        } else {
            ts.cameraIntrinsicsPublisher.accept(new double[0]);
            ts.cameraDistortionPublisher.accept(new double[0]);
        }

        ts.heartbeatPublisher.set(acceptedResult.sequenceID);

        // TODO...nt4... is this needed?
        rootTable.getInstance().flush();
    }
}

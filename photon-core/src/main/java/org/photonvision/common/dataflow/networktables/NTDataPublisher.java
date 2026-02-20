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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTablesJNI;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CompositePipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final Logger logger = new Logger(NTDataPublisher.class, LogGroup.General);

    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;

    private final NTTopicSet ts = new NTTopicSet();
    private final NTTopicSet tagsTs = new NTTopicSet();
    private final NTTopicSet objectsTs = new NTTopicSet();

    NTDataChangeListener pipelineIndexListener;
    private final Supplier<Integer> pipelineIndexSupplier;
    private final Consumer<Integer> pipelineIndexConsumer;

    NTDataChangeListener driverModeListener;
    private final BooleanSupplier driverModeSupplier;
    private final Consumer<Boolean> driverModeConsumer;

    NTDataChangeListener fpsLimitListener;
    private final Consumer<Integer> fpsLimitConsumer;
    private final Supplier<Integer> fpsLimitSupplier;

    public NTDataPublisher(
            String cameraNickname,
            Supplier<Integer> pipelineIndexSupplier,
            Consumer<Integer> pipelineIndexConsumer,
            BooleanSupplier driverModeSupplier,
            Consumer<Boolean> driverModeConsumer,
            Supplier<Integer> fpsLimitSupplier,
            Consumer<Integer> fpsLimitConsumer) {
        this.pipelineIndexSupplier = pipelineIndexSupplier;
        this.pipelineIndexConsumer = pipelineIndexConsumer;
        this.driverModeSupplier = driverModeSupplier;
        this.driverModeConsumer = driverModeConsumer;
        this.fpsLimitSupplier = fpsLimitSupplier;
        this.fpsLimitConsumer = fpsLimitConsumer;

        updateCameraNickname(cameraNickname);
        updateEntries();
    }

    private void onPipelineIndexChange(NetworkTableEvent entryNotification) {
        var newIndex = (int) entryNotification.valueData.value.getInteger();
        var originalIndex = pipelineIndexSupplier.get();

        // ignore indexes below 0
        if (newIndex < 0) {
            ts.pipelineIndexPublisher.set(originalIndex);
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

    private void onFPSLimitChange(NetworkTableEvent entryNotification) {
        var newFPSLimit = (int) entryNotification.valueData.value.getInteger();
        var originalFPSLimit = fpsLimitSupplier.get();

        if (newFPSLimit == originalFPSLimit) {
            logger.debug("FPS limit is already " + newFPSLimit);
            return;
        }

        fpsLimitConsumer.accept(newFPSLimit);
        logger.debug("Set FPS limit to " + newFPSLimit);
    }

    private void removeEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();
        ts.removeEntries();
        tagsTs.removeEntries();
        objectsTs.removeEntries();
    }

    private void updateEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();
        if (fpsLimitListener != null) fpsLimitListener.remove();

        ts.updateEntries();
        tagsTs.updateEntries();
        objectsTs.updateEntries();

        pipelineIndexListener =
                new NTDataChangeListener(
                        ts.subTable.getInstance(), ts.pipelineIndexRequestSub, this::onPipelineIndexChange);

        driverModeListener =
                new NTDataChangeListener(
                        ts.subTable.getInstance(), ts.driverModeSubscriber, this::onDriverModeChange);

        fpsLimitListener =
                new NTDataChangeListener(
                        ts.subTable.getInstance(), ts.fpsLimitSubscriber, this::onFPSLimitChange);
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        ts.subTable = rootTable.getSubTable(newCameraNickname);
        tagsTs.subTable = rootTable.getSubTable(newCameraNickname + "-tags");
        objectsTs.subTable = rootTable.getSubTable(newCameraNickname + "-objects");
        updateEntries();
    }

    @Override
    public void accept(CVPipelineResult result) {
        CVPipelineResult acceptedResult;
        if (result instanceof CalibrationPipelineResult) {
            // If the data is from a calibration pipeline, override the list of targets to be null to
            // prevent the data from being sent and continue to post blank/zero data to the network tables
            acceptedResult =
                    new CVPipelineResult(
                            result.sequenceID,
                            result.processingNanos,
                            result.fps,
                            List.of(),
                            result.inputAndOutputFrame);
        } else {
            acceptedResult = result;
        }
        var now = NetworkTablesJNI.now();
        var captureMicros = MathUtils.nanosToMicros(result.getImageCaptureTimestampNanos());

        var offset = NetworkTablesManager.getInstance().getOffset();

        publishToTopicSet(
                ts,
                acceptedResult,
                acceptedResult.targets,
                acceptedResult.multiTagResult,
                captureMicros,
                now,
                offset);

        if (result instanceof CompositePipelineResult compositeResult) {
            publishToTopicSet(
                    tagsTs,
                    acceptedResult,
                    compositeResult.aprilTagTargets,
                    acceptedResult.multiTagResult,
                    captureMicros,
                    now,
                    offset);
            publishToTopicSet(
                    objectsTs,
                    acceptedResult,
                    compositeResult.objectDetectionTargets,
                    Optional.<MultiTargetPNPResult>empty(),
                    captureMicros,
                    now,
                    offset);
        } else {
            publishToTopicSet(
                    tagsTs,
                    acceptedResult,
                    List.<TrackedTarget>of(),
                    Optional.<MultiTargetPNPResult>empty(),
                    captureMicros,
                    now,
                    offset);
            publishToTopicSet(
                    objectsTs,
                    acceptedResult,
                    List.<TrackedTarget>of(),
                    Optional.<MultiTargetPNPResult>empty(),
                    captureMicros,
                    now,
                    offset);
        }

        // TODO...nt4... is this needed?
        rootTable.getInstance().flush();
    }

    private void publishToTopicSet(
            NTTopicSet topics,
            CVPipelineResult result,
            List<TrackedTarget> targets,
            Optional<MultiTargetPNPResult> multiTagResult,
            long captureMicros,
            long now,
            long offset) {
        var safeTargets = targets != null ? targets : List.<TrackedTarget>of();

        var simplified =
                new PhotonPipelineResult(
                        result.sequenceID,
                        captureMicros + offset,
                        now + offset,
                        NetworkTablesManager.getInstance().getTimeSinceLastPong(),
                        TrackedTarget.simpleFromTrackedTargets(safeTargets),
                        multiTagResult);

        topics.resultPublisher.set(simplified, 1024);
        if (ConfigManager.getInstance().getConfig().getNetworkConfig().shouldPublishProto) {
            topics.protoResultPublisher.set(simplified);
        }

        topics.pipelineIndexPublisher.set(pipelineIndexSupplier.get());
        topics.driverModePublisher.set(driverModeSupplier.getAsBoolean());
        topics.fpsLimitPublisher.set(fpsLimitSupplier.get());
        topics.latencyMillisEntry.set(result.getLatencyMillis());
        topics.fpsEntry.set(result.fps);
        topics.hasTargetEntry.set(!safeTargets.isEmpty());

        if (!safeTargets.isEmpty()) {
            var bestTarget = safeTargets.get(0);

            topics.targetPitchEntry.set(bestTarget.getPitch());
            topics.targetYawEntry.set(bestTarget.getYaw());
            topics.targetAreaEntry.set(bestTarget.getArea());
            topics.targetSkewEntry.set(bestTarget.getSkew());

            var pose = bestTarget.getBestCameraToTarget3d();
            topics.targetPoseEntry.set(pose);

            var targetOffsetPoint = bestTarget.getTargetOffsetPoint();
            topics.bestTargetPosX.set(targetOffsetPoint.x);
            topics.bestTargetPosY.set(targetOffsetPoint.y);
        } else {
            topics.targetPitchEntry.set(0);
            topics.targetYawEntry.set(0);
            topics.targetAreaEntry.set(0);
            topics.targetSkewEntry.set(0);
            topics.targetPoseEntry.set(new Transform3d());
            topics.bestTargetPosX.set(0);
            topics.bestTargetPosY.set(0);
        }

        if (result.inputAndOutputFrame != null
                && result.inputAndOutputFrame.frameStaticProperties != null
                && result.inputAndOutputFrame.frameStaticProperties.cameraCalibration != null) {
            var fsp = result.inputAndOutputFrame.frameStaticProperties;
            topics.cameraIntrinsicsPublisher.accept(fsp.cameraCalibration.getIntrinsicsArr());
            topics.cameraDistortionPublisher.accept(fsp.cameraCalibration.getDistCoeffsArr());
        } else {
            topics.cameraIntrinsicsPublisher.accept(new double[0]);
            topics.cameraDistortionPublisher.accept(new double[0]);
        }

        topics.heartbeatPublisher.set(result.sequenceID);
    }
}

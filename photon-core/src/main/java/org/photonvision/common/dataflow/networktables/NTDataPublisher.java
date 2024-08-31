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

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.util.WPIUtilJNI;
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
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final Logger logger = new Logger(NTDataPublisher.class, LogGroup.General);

    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;

    private final NTTopicSet ts = new NTTopicSet();

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

    private void removeEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();
        ts.removeEntries();
    }

    private void updateEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();

        ts.updateEntries();

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
        updateEntries();
    }

    @Override
    public void accept(CVPipelineResult result) {
        var now = WPIUtilJNI.now();
        var captureMicros = MathUtils.nanosToMicros(result.getImageCaptureTimestampNanos());
        var simplified =
                new PhotonPipelineResult(
                        result.sequenceID,
                        captureMicros,
                        now,
                        TrackedTarget.simpleFromTrackedTargets(result.targets),
                        result.multiTagResult);

        // random guess at size of the array
        ts.resultPublisher.set(simplified, 1024);
        if (ConfigManager.getInstance().getConfig().getNetworkConfig().shouldPublishProto) {
            ts.protoResultPublisher.set(simplified);
        }

        ts.pipelineIndexPublisher.set(pipelineIndexSupplier.get());
        ts.driverModePublisher.set(driverModeSupplier.getAsBoolean());
        ts.latencyMillisEntry.set(result.getLatencyMillis());
        ts.hasTargetEntry.set(result.hasTargets());

        if (result.hasTargets()) {
            var bestTarget = result.targets.get(0);

            ts.targetPitchEntry.set(bestTarget.getPitch());
            ts.targetYawEntry.set(bestTarget.getYaw());
            ts.targetAreaEntry.set(bestTarget.getArea());
            ts.targetSkewEntry.set(bestTarget.getSkew());

            var pose = bestTarget.getBestCameraToTarget3d();
            ts.targetPoseEntry.set(
                    new double[] {
                        pose.getTranslation().getX(),
                        pose.getTranslation().getY(),
                        pose.getTranslation().getZ(),
                        pose.getRotation().getQuaternion().getW(),
                        pose.getRotation().getQuaternion().getX(),
                        pose.getRotation().getQuaternion().getY(),
                        pose.getRotation().getQuaternion().getZ()
                    });

            var targetOffsetPoint = bestTarget.getTargetOffsetPoint();
            ts.bestTargetPosX.set(targetOffsetPoint.x);
            ts.bestTargetPosY.set(targetOffsetPoint.y);
        } else {
            ts.targetPitchEntry.set(0);
            ts.targetYawEntry.set(0);
            ts.targetAreaEntry.set(0);
            ts.targetSkewEntry.set(0);
            ts.targetPoseEntry.set(new double[] {0, 0, 0});
            ts.bestTargetPosX.set(0);
            ts.bestTargetPosY.set(0);
        }

        // Something in the result can sometimes be null -- so check probably too many things
        if (result.inputAndOutputFrame != null
                && result.inputAndOutputFrame.frameStaticProperties != null
                && result.inputAndOutputFrame.frameStaticProperties.cameraCalibration != null) {
            var fsp = result.inputAndOutputFrame.frameStaticProperties;
            ts.cameraIntrinsicsPublisher.accept(fsp.cameraCalibration.getIntrinsicsArr());
            ts.cameraDistortionPublisher.accept(fsp.cameraCalibration.getDistCoeffsArr());
        } else {
            ts.cameraIntrinsicsPublisher.accept(new double[] {});
            ts.cameraDistortionPublisher.accept(new double[] {});
        }

        ts.heartbeatPublisher.set(result.sequenceID);

        // TODO...nt4... is this needed?
        rootTable.getInstance().flush();
    }
}

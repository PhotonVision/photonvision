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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final Logger logger = new Logger(NTDataPublisher.class, LogGroup.General);

    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;

    private NTTopicSet ts = new NTTopicSet();

    NTDataChangeListener pipelineIndexListener;
    private final Supplier<Integer> pipelineIndexSupplier;
    private final Consumer<Integer> pipelineIndexConsumer;

    NTDataChangeListener driverModeListener;
    private final BooleanSupplier driverModeSupplier;
    private final Consumer<Boolean> driverModeConsumer;

    private long heartbeatCounter = 0;

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
        logger.debug("Successfully set pipeline index to " + newIndex);
    }

    private void onDriverModeChange(NetworkTableEvent entryNotification) {
        var newDriverMode = entryNotification.valueData.value.getBoolean();
        var originalDriverMode = driverModeSupplier.getAsBoolean();

        if (newDriverMode == originalDriverMode) {
            logger.debug("Driver mode is already " + newDriverMode);
            return;
        }

        driverModeConsumer.accept(newDriverMode);
        logger.debug("Successfully set driver mode to " + newDriverMode);
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
                        ts.subTable.getInstance(), ts.pipelineIndexSubscriber, this::onPipelineIndexChange);

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
        var simplified =
                new PhotonPipelineResult(
                        result.getLatencyMillis(), simpleFromTrackedTargets(result.targets));
        Packet packet = new Packet(simplified.getPacketSize());
        simplified.populatePacket(packet);

        ts.rawBytesEntry.set(packet.getData());

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

        ts.heartbeatPublisher.set(heartbeatCounter++);

        // TODO...nt4... is this needed?
        rootTable.getInstance().flush();
    }

    public static List<PhotonTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<PhotonTrackedTarget>();
        for (var t : targets) {
            var minAreaRectCorners = new ArrayList<TargetCorner>();
            var detectedCorners = new ArrayList<TargetCorner>();
            {
                var points = new Point[4];
                t.getMinAreaRect().points(points);
                for (int i = 0; i < 4; i++) {
                    minAreaRectCorners.add(new TargetCorner(points[i].x, points[i].y));
                }
            }
            {
                var points = t.getTargetCorners();
                for (int i = 0; i < points.size(); i++) {
                    detectedCorners.add(new TargetCorner(points.get(i).x, points.get(i).y));
                }
            }

            ret.add(
                    new PhotonTrackedTarget(
                            t.getYaw(),
                            t.getPitch(),
                            t.getArea(),
                            t.getSkew(),
                            t.getFiducialId(),
                            t.getBestCameraToTarget3d(),
                            t.getAltCameraToTarget3d(),
                            t.getPoseAmbiguity(),
                            minAreaRectCorners,
                            detectedCorners));
        }
        return ret;
    }
}

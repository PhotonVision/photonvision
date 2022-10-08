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

import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;
    private NetworkTable subTable;
    private NetworkTableEntry rawBytesEntry;

    private NetworkTableEntry pipelineIndexEntry;
    private final Consumer<Integer> pipelineIndexConsumer;
    private NTDataChangeListener pipelineIndexListener;
    private NetworkTableEntry driverModeEntry;
    private final Consumer<Boolean> driverModeConsumer;
    private NTDataChangeListener driverModeListener;

    private NetworkTableEntry latencyMillisEntry;
    private NetworkTableEntry hasTargetEntry;
    private NetworkTableEntry targetPitchEntry;
    private NetworkTableEntry targetYawEntry;
    private NetworkTableEntry targetAreaEntry;
    private NetworkTableEntry targetPoseEntry;
    private NetworkTableEntry targetSkewEntry;

    // The raw position of the best target, in pixels.
    private NetworkTableEntry bestTargetPosX;
    private NetworkTableEntry bestTargetPosY;

    private final Supplier<Integer> pipelineIndexSupplier;
    private final BooleanSupplier driverModeSupplier;

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

    private void onPipelineIndexChange(EntryNotification entryNotification) {
        var newIndex = (int) entryNotification.value.getDouble();
        var originalIndex = pipelineIndexSupplier.get();

        // ignore indexes below 0
        if (newIndex < 0) {
            pipelineIndexEntry.forceSetNumber(originalIndex);
            return;
        }

        if (newIndex == originalIndex) {
            // TODO: Log
            return;
        }

        pipelineIndexConsumer.accept(newIndex);
        var setIndex = pipelineIndexSupplier.get();
        if (newIndex != setIndex) { // set failed
            pipelineIndexEntry.forceSetNumber(setIndex);
            // TODO: Log
        }
        // TODO: Log
    }

    private void onDriverModeChange(EntryNotification entryNotification) {
        var newDriverMode = entryNotification.value.getBoolean();
        var originalDriverMode = driverModeSupplier.getAsBoolean();

        if (newDriverMode == originalDriverMode) {
            // TODO: Log
            return;
        }

        driverModeConsumer.accept(newDriverMode);
        // TODO: Log
    }

    @SuppressWarnings("DuplicatedCode")
    private void removeEntries() {
        if (rawBytesEntry != null) rawBytesEntry.delete();
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (pipelineIndexEntry != null) pipelineIndexEntry.delete();
        if (driverModeListener != null) driverModeListener.remove();
        if (driverModeEntry != null) driverModeEntry.delete();
        if (latencyMillisEntry != null) latencyMillisEntry.delete();
        if (hasTargetEntry != null) hasTargetEntry.delete();
        if (targetPitchEntry != null) targetPitchEntry.delete();
        if (targetAreaEntry != null) targetAreaEntry.delete();
        if (targetYawEntry != null) targetYawEntry.delete();
        if (targetPoseEntry != null) targetPoseEntry.delete();
        if (targetSkewEntry != null) targetSkewEntry.delete();
        if (bestTargetPosX != null) bestTargetPosX.delete();
        if (bestTargetPosY != null) bestTargetPosY.delete();
    }

    private void updateEntries() {
        rawBytesEntry = subTable.getEntry("rawBytes");

        if (pipelineIndexListener != null) {
            pipelineIndexListener.remove();
        }
        pipelineIndexEntry = subTable.getEntry("pipelineIndex");
        pipelineIndexListener =
                new NTDataChangeListener(pipelineIndexEntry, this::onPipelineIndexChange);

        if (driverModeListener != null) {
            driverModeListener.remove();
        }
        driverModeEntry = subTable.getEntry("driverMode");
        driverModeListener = new NTDataChangeListener(driverModeEntry, this::onDriverModeChange);

        latencyMillisEntry = subTable.getEntry("latencyMillis");
        hasTargetEntry = subTable.getEntry("hasTarget");

        targetPitchEntry = subTable.getEntry("targetPitch");
        targetAreaEntry = subTable.getEntry("targetArea");
        targetYawEntry = subTable.getEntry("targetYaw");
        targetPoseEntry = subTable.getEntry("targetPose");
        targetSkewEntry = subTable.getEntry("targetSkew");

        bestTargetPosX = subTable.getEntry("targetPixelsX");
        bestTargetPosY = subTable.getEntry("targetPixelsY");
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        subTable = rootTable.getSubTable(newCameraNickname);
        updateEntries();
    }

    @Override
    public void accept(CVPipelineResult result) {
        var simplified =
                new PhotonPipelineResult(
                        result.getLatencyMillis(), simpleFromTrackedTargets(result.targets));
        Packet packet = new Packet(simplified.getPacketSize());
        simplified.populatePacket(packet);

        rawBytesEntry.forceSetRaw(packet.getData());

        pipelineIndexEntry.forceSetNumber(pipelineIndexSupplier.get());
        driverModeEntry.forceSetBoolean(driverModeSupplier.getAsBoolean());
        latencyMillisEntry.forceSetDouble(result.getLatencyMillis());
        hasTargetEntry.forceSetBoolean(result.hasTargets());

        if (result.hasTargets()) {
            var bestTarget = result.targets.get(0);

            targetPitchEntry.forceSetDouble(bestTarget.getPitch());
            targetYawEntry.forceSetDouble(bestTarget.getYaw());
            targetAreaEntry.forceSetDouble(bestTarget.getArea());
            targetSkewEntry.forceSetDouble(bestTarget.getSkew());

            var pose = bestTarget.getCameraToTarget3d();
            targetPoseEntry.forceSetDoubleArray(
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
            bestTargetPosX.forceSetDouble(targetOffsetPoint.x);
            bestTargetPosY.forceSetDouble(targetOffsetPoint.y);
        } else {
            targetPitchEntry.forceSetDouble(0);
            targetYawEntry.forceSetDouble(0);
            targetAreaEntry.forceSetDouble(0);
            targetSkewEntry.forceSetDouble(0);
            targetPoseEntry.forceSetDoubleArray(new double[] {0, 0, 0});
            bestTargetPosX.forceSetDouble(0);
            bestTargetPosY.forceSetDouble(0);
        }
        rootTable.getInstance().flush();
    }

    public static List<PhotonTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<PhotonTrackedTarget>();
        for (var t : targets) {
            var points = new Point[4];
            t.getMinAreaRect().points(points);
            var cornerList = new ArrayList<TargetCorner>();

            for (int i = 0; i < 4; i++) cornerList.add(new TargetCorner(points[i].x, points[i].y));

            ret.add(
                    new PhotonTrackedTarget(
                            t.getYaw(),
                            t.getPitch(),
                            t.getArea(),
                            t.getSkew(),
                            t.getFiducialId(),
                            t.getCameraToTarget3d(),
                            t.getPoseAmbiguity(),
                            cornerList));
        }
        return ret;
    }
}

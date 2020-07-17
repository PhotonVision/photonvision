/*
 * Copyright (C) 2020 Photon Vision.
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.SimplePipelineResult;

public class NTDataPublisher implements CVPipelineResultConsumer {

    private final NetworkTable rootTable = NetworkTablesManager.kRootTable;
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

    private final Supplier<Integer> pipelineIndexSupplier;
    private final BooleanSupplier driverModeSupplier;

    private String currentCameraNickname;

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

        currentCameraNickname = cameraNickname;
        updateCameraNickname(cameraNickname);
        updateEntries();
    }

    private void onPipelineIndexChange(EntryNotification entryNotification) {
        var newIndex = (int) entryNotification.value.getDouble();
        var originalIndex = pipelineIndexSupplier.get();

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

    private void removeEntries() {
        rawBytesEntry.delete();

        if (pipelineIndexListener != null) {
            pipelineIndexListener.remove();
        }
        pipelineIndexEntry.delete();

        if (driverModeListener != null) {
            driverModeListener.remove();
        }
        driverModeEntry.delete();

        latencyMillisEntry.delete();
        hasTargetEntry.delete();

        targetPitchEntry.delete();
        targetAreaEntry.delete();
        targetYawEntry.delete();
        targetPoseEntry.delete();
        targetSkewEntry.delete();
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
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        subTable = rootTable.getSubTable(newCameraNickname);
        updateEntries();
        currentCameraNickname = newCameraNickname;
    }

    @Override
    public void accept(CVPipelineResult result) {
        var simplified = new SimplePipelineResult(result);
        var bytes = simplified.toByteArray();
        rawBytesEntry.forceSetRaw(bytes);

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

            var poseX = bestTarget.getRobotRelativePose().getTranslation().getX();
            var poseY = bestTarget.getRobotRelativePose().getTranslation().getY();
            var poseRot = bestTarget.getRobotRelativePose().getRotation().getDegrees();
            targetPoseEntry.forceSetDoubleArray(new double[] {poseX, poseY, poseRot});
        } else {
            targetPitchEntry.forceSetDouble(0);
            targetYawEntry.forceSetDouble(0);
            targetAreaEntry.forceSetDouble(0);
            targetSkewEntry.forceSetDouble(0);
            targetPoseEntry.forceSetDoubleArray(new double[] {0, 0, 0});
        }
        rootTable.getInstance().flush();
    }
}

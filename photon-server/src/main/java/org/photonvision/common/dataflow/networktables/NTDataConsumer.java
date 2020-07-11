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

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import org.photonvision.common.datatransfer.DataConsumer;
import org.photonvision.vision.pipeline.result.SimplePipelineResult;
import org.photonvision.vision.processes.Data;

public class NTDataConsumer implements DataConsumer {

    private final NetworkTable rootTable;
    private NetworkTable subTable;
    private NetworkTableEntry rawDataEntry;
    private NetworkTableEntry pipelineIndexEntry;
    public NetworkTableEntry driverModeEntry;
    //private int ntDriveModeListenerID; // TODO: NTDataPublisher
    //private int ntPipelineListenerID; // TODO: NTDataPublisher
    private NetworkTableEntry latencyMillisEntry;
    private NetworkTableEntry hasTargetEntry;
    private NetworkTableEntry targetPitchEntry;
    private NetworkTableEntry targetYawEntry;
    private NetworkTableEntry targetAreaEntry;
    private NetworkTableEntry targetPoseEntry;

    // TODO: what are these, and do we need them?
    private NetworkTableEntry targetFittedHeightEntry;
    private NetworkTableEntry targetFittedWidthEntry;
    private NetworkTableEntry targetBoundingHeightEntry;
    private NetworkTableEntry targetBoundingWidthEntry;
    private NetworkTableEntry targetRotationEntry;

    public NTDataConsumer(NetworkTable root, String camName) {
        rootTable = root;
        subTable = root.getSubTable(camName);
        rawDataEntry = subTable.getEntry("rawData");

        pipelineIndexEntry = subTable.getEntry("pipelineIndex");
        driverModeEntry = subTable.getEntry("driverMode");
        latencyMillisEntry = subTable.getEntry("latencyMillis");
        hasTargetEntry = subTable.getEntry("hasTarget");

        targetPitchEntry = subTable.getEntry("targetPitch");
        targetAreaEntry = subTable.getEntry("targetArea");
        targetYawEntry = subTable.getEntry("targetYaw");
        targetPoseEntry = subTable.getEntry("targetPose");
//        targetFittedHeightEntry = subTable.getEntry("targetFittedHeight");
//        targetFittedWidthEntry = subTable.getEntry("targetFittedWidth");
//        targetBoundingHeightEntry = subTable.getEntry("targetBoundingHeight");
//        targetBoundingWidthEntry = subTable.getEntry("targetBoundingWidth");
//        targetRotationEntry = subTable.getEntry("targetRotation");
    }

    public void setCameraName(String camName) {
        subTable = rootTable.getSubTable(camName);
        rawDataEntry = subTable.getEntry("rawData");

        pipelineIndexEntry = subTable.getEntry("pipelineIndex");
        driverModeEntry = subTable.getEntry("driverMode");
        latencyMillisEntry = subTable.getEntry("latencyMillis");
        hasTargetEntry = subTable.getEntry("hasTarget");

        targetPitchEntry = subTable.getEntry("targetPitch");
        targetAreaEntry = subTable.getEntry("targetArea");
        targetYawEntry = subTable.getEntry("targetYaw");
        targetPoseEntry = subTable.getEntry("targetPose");
//        targetFittedHeightEntry = subTable.getEntry("targetFittedHeight");
//        targetFittedWidthEntry = subTable.getEntry("targetFittedWidth");
//        targetBoundingHeightEntry = subTable.getEntry("targetBoundingHeight");
//        targetBoundingWidthEntry = subTable.getEntry("targetBoundingWidth");
        //targetRotationEntry = subTable.getEntry("targetRotation")
    }

    @Override
    public void accept(Data data) {
        var result = data.result;
        // send raw data immediately
        var simplified = new SimplePipelineResult(result);
        var bytes = simplified.toByteArray();
        rawDataEntry.forceSetRaw(bytes);
        rootTable.getInstance().flush();

        // send keyed data after raw
        // TODO: get pipelineIndex, driverMode

        latencyMillisEntry.forceSetDouble(result.getLatencyMillis());
        hasTargetEntry.forceSetBoolean(result.hasTargets());

        // TODO: send all zeroes if no targets?
        var bestTarget = result.targets.get(0);

        targetPitchEntry.forceSetDouble(bestTarget.getPitch());
        targetYawEntry.forceSetDouble(bestTarget.getYaw());
        targetAreaEntry.forceSetDouble(bestTarget.getArea());

        var poseX = bestTarget.getRobotRelativePose().getTranslation().getX();
        var poseY = bestTarget.getRobotRelativePose().getTranslation().getY();
        var poseRot = bestTarget.getRobotRelativePose().getRotation().getDegrees();
        targetPoseEntry.forceSetDoubleArray(new double[] { poseX, poseY, poseRot });
    }
}

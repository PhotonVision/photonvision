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

package org.photonvision.common.networktables;

import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.BooleanTopic;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.RawPublisher;

/**
 * This class is a wrapper around all per-pipeline NT topics that PhotonVision should be publishing
 * It's split here so the sim and real-camera implementations can share a common implementation of
 * the naming and registration of the NT content.
 *
 * <p>However, we do expect that the actual logic which fills out values in the entries will be
 * different for sim vs. real camera
 */
public class NTTopicSet {
    public NetworkTable subTable;
    public RawPublisher rawBytesEntry;

    public IntegerTopic pipelineIndexTopic;
    public IntegerPublisher pipelineIndexPublisher;
    public IntegerSubscriber pipelineIndexSubscriber;

    public BooleanTopic driverModeEntry;
    public BooleanPublisher driverModePublisher;
    public BooleanSubscriber driverModeSubscriber;

    public DoublePublisher latencyMillisEntry;
    public BooleanPublisher hasTargetEntry;
    public DoublePublisher targetPitchEntry;
    public DoublePublisher targetYawEntry;
    public DoublePublisher targetAreaEntry;
    public DoubleArrayPublisher targetPoseEntry;
    public DoublePublisher targetSkewEntry;

    // The raw position of the best target, in pixels.
    public DoublePublisher bestTargetPosX;
    public DoublePublisher bestTargetPosY;

    // Heartbeat
    public IntegerTopic heartbeatTopic;
    public IntegerPublisher heartbeatPublisher;

    public void updateEntries() {
        rawBytesEntry =
                subTable
                        .getRawTopic("rawBytes")
                        .publish("rawBytes", PubSubOption.periodic(0.01), PubSubOption.sendAll(true));

        pipelineIndexTopic = subTable.getIntegerTopic("pipelineIndex");
        pipelineIndexPublisher = pipelineIndexTopic.publish();
        pipelineIndexSubscriber = pipelineIndexTopic.subscribe(0);

        driverModePublisher = subTable.getBooleanTopic("driverMode").publish();
        driverModeSubscriber = subTable.getBooleanTopic("driverModeRequest").subscribe(false);

        // Fun little hack to make the request show up
        driverModeSubscriber.getTopic().publish().setDefault(false);

        latencyMillisEntry = subTable.getDoubleTopic("latencyMillis").publish();
        hasTargetEntry = subTable.getBooleanTopic("hasTarget").publish();

        targetPitchEntry = subTable.getDoubleTopic("targetPitch").publish();
        targetAreaEntry = subTable.getDoubleTopic("targetArea").publish();
        targetYawEntry = subTable.getDoubleTopic("targetYaw").publish();
        targetPoseEntry = subTable.getDoubleArrayTopic("targetPose").publish();
        targetSkewEntry = subTable.getDoubleTopic("targetSkew").publish();

        bestTargetPosX = subTable.getDoubleTopic("targetPixelsX").publish();
        bestTargetPosY = subTable.getDoubleTopic("targetPixelsY").publish();

        heartbeatTopic = subTable.getIntegerTopic("heartbeat");
        heartbeatPublisher = heartbeatTopic.publish();
    }

    @SuppressWarnings("DuplicatedCode")
    public void removeEntries() {
        if (rawBytesEntry != null) rawBytesEntry.close();
        if (pipelineIndexPublisher != null) pipelineIndexPublisher.close();
        if (pipelineIndexSubscriber != null) pipelineIndexSubscriber.close();

        if (driverModePublisher != null) driverModePublisher.close();
        if (driverModeSubscriber != null) driverModeSubscriber.close();

        if (latencyMillisEntry != null) latencyMillisEntry.close();
        if (hasTargetEntry != null) hasTargetEntry.close();
        if (targetPitchEntry != null) targetPitchEntry.close();
        if (targetAreaEntry != null) targetAreaEntry.close();
        if (targetYawEntry != null) targetYawEntry.close();
        if (targetPoseEntry != null) targetPoseEntry.close();
        if (targetSkewEntry != null) targetSkewEntry.close();
        if (bestTargetPosX != null) bestTargetPosX.close();
        if (bestTargetPosY != null) bestTargetPosY.close();
    }
}

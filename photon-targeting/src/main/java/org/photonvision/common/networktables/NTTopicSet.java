/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

package org.photonvision.common.networktables;

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.BooleanTopic;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.ProtobufPublisher;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.StructPublisher;
import org.photonvision.targeting.PhotonPipelineResult;

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

    public PacketPublisher<PhotonPipelineResult> resultPublisher;
    public ProtobufPublisher<PhotonPipelineResult> protoResultPublisher;

    public IntegerPublisher pipelineIndexPublisher;
    public IntegerSubscriber pipelineIndexRequestSub;

    public BooleanTopic driverModeEntry;
    public BooleanPublisher driverModePublisher;
    public BooleanSubscriber driverModeSubscriber;

    public DoublePublisher latencyMillisEntry;
    public BooleanPublisher hasTargetEntry;
    public DoublePublisher targetPitchEntry;
    public DoublePublisher targetYawEntry;
    public DoublePublisher targetAreaEntry;
    public StructPublisher<Transform3d> targetPoseEntry;
    public DoublePublisher targetSkewEntry;

    // The raw position of the best target, in pixels.
    public DoublePublisher bestTargetPosX;
    public DoublePublisher bestTargetPosY;

    // Heartbeat
    public IntegerTopic heartbeatTopic;
    public IntegerPublisher heartbeatPublisher;

    // Camera Calibration
    public DoubleArrayPublisher cameraIntrinsicsPublisher;
    public DoubleArrayPublisher cameraDistortionPublisher;

    public void updateEntries() {
        var rawBytesEntry =
                subTable
                        .getRawTopic("rawBytes")
                        .publish(
                                PhotonPipelineResult.photonStruct.getTypeString(),
                                PubSubOption.periodic(0.01),
                                PubSubOption.sendAll(true),
                                PubSubOption.keepDuplicates(true));

        resultPublisher =
                new PacketPublisher<PhotonPipelineResult>(rawBytesEntry, PhotonPipelineResult.photonStruct);
        protoResultPublisher =
                subTable
                        .getProtobufTopic("result_proto", PhotonPipelineResult.proto)
                        .publish(PubSubOption.periodic(0.01), PubSubOption.sendAll(true));

        pipelineIndexPublisher = subTable.getIntegerTopic("pipelineIndexState").publish();
        pipelineIndexRequestSub = subTable.getIntegerTopic("pipelineIndexRequest").subscribe(0);

        driverModePublisher = subTable.getBooleanTopic("driverMode").publish();
        driverModeSubscriber = subTable.getBooleanTopic("driverModeRequest").subscribe(false);

        // Fun little hack to make the request show up
        driverModeSubscriber.getTopic().publish().setDefault(false);

        latencyMillisEntry = subTable.getDoubleTopic("latencyMillis").publish();
        hasTargetEntry = subTable.getBooleanTopic("hasTarget").publish();

        targetPitchEntry = subTable.getDoubleTopic("targetPitch").publish();
        targetAreaEntry = subTable.getDoubleTopic("targetArea").publish();
        targetYawEntry = subTable.getDoubleTopic("targetYaw").publish();
        targetPoseEntry = subTable.getStructTopic("targetPose", Transform3d.struct).publish();
        targetSkewEntry = subTable.getDoubleTopic("targetSkew").publish();

        bestTargetPosX = subTable.getDoubleTopic("targetPixelsX").publish();
        bestTargetPosY = subTable.getDoubleTopic("targetPixelsY").publish();

        heartbeatTopic = subTable.getIntegerTopic("heartbeat");
        heartbeatPublisher = heartbeatTopic.publish();

        cameraIntrinsicsPublisher = subTable.getDoubleArrayTopic("cameraIntrinsics").publish();
        cameraDistortionPublisher = subTable.getDoubleArrayTopic("cameraDistortion").publish();
    }

    @SuppressWarnings("DuplicatedCode")
    public void removeEntries() {
        if (resultPublisher != null) resultPublisher.close();
        if (pipelineIndexPublisher != null) pipelineIndexPublisher.close();
        if (pipelineIndexRequestSub != null) pipelineIndexRequestSub.close();

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

        if (heartbeatPublisher != null) heartbeatPublisher.close();

        if (cameraIntrinsicsPublisher != null) cameraIntrinsicsPublisher.close();
        if (cameraDistortionPublisher != null) cameraDistortionPublisher.close();
    }
}

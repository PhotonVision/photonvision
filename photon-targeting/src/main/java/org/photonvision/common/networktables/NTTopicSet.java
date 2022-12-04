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
import edu.wpi.first.networktables.RawPublisher;

/**
 * This class is a wrapper around all per-pipeline NT topics that PhotonVision should be publishing
 * It's split here so the sim and real-camera implementations can share a common implementation
 * of the naming and registration of the NT content.
 * 
 * However, we do expect that the actual logic which fills out values in the entries
 * will be different for sim vs. real camera
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
    

    public void updateEntries(){
        rawBytesEntry =
        subTable.getRawTopic("rawBytes").publish("rawBytes");

        pipelineIndexTopic = subTable.getIntegerTopic("pipelineIndex");
        pipelineIndexPublisher = pipelineIndexTopic.publish();
        pipelineIndexSubscriber = pipelineIndexTopic.subscribe(0);

        driverModeEntry = subTable.getBooleanTopic("driverMode");
        driverModePublisher = driverModeEntry.publish();
        driverModeSubscriber = driverModeEntry.subscribe(false);

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
        heartbeatPublisher = pipelineIndexTopic.publish();
    }

    @SuppressWarnings("DuplicatedCode")
    public void removeEntries(){
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

package org.photonvision.targeting;

import org.photonvision.struct.PhotonPacketSerdeStruct;

public class PhotonPipelineMetadata {
    // Mirror of the heartbeat entry -- monotonically increasing
    public long sequenceID;

    // Image capture and NT publish timestamp, in microseconds and in the
    // coprocessor timebase. As
    // reported by WPIUtilJNI::now.
    public long captureTimestampMicros;
    public long publishTimestampMicros;

    public PhotonPipelineMetadata(
            long captureTimestampMicros, long publishTimestampMicros, long sequenceID) {
        this.captureTimestampMicros = captureTimestampMicros;
        this.publishTimestampMicros = publishTimestampMicros;
        this.sequenceID = sequenceID;
    }

    public PhotonPipelineMetadata() {
        this(-1, -1, -1);
    }

    /** Returns the time between image capture and publish to NT */
    public double getLatencyMillis() {
        return (publishTimestampMicros - captureTimestampMicros) / 1e3;
    }

    /** The time that this image was captured, in the coprocessor's time base. */
    public long getCaptureTimestampMicros() {
        return captureTimestampMicros;
    }

    /** The time that this result was published to NT, in the coprocessor's time base. */
    public long getPublishTimestampMicros() {
        return publishTimestampMicros;
    }

    /**
     * The number of non-empty frames processed by this camera since boot. Useful to checking if a
     * camera is alive.
     */
    public long getSequenceID() {
        return sequenceID;
    }

    PhotonPipelineResultMetadataSerde serde = new PhotonPipelineResultMetadataSerde();
}

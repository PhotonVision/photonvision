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

package org.photonvision.targeting;

import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.PhotonPipelineMetadataSerde;
import org.photonvision.targeting.serde.PhotonStructSerializable;

public class PhotonPipelineMetadata implements PhotonStructSerializable<PhotonPipelineMetadata> {
    // Image capture and NT publish timestamp, in microseconds
    // The timebase is nt::Now on the time sync server
    public long captureTimestampMicros;
    public long publishTimestampMicros;

    // Mirror of the heartbeat entry -- monotonically increasing
    public long sequenceID;

    // Time from last Time Sync Pong received and the construction of this metadata, in uS
    public long timeSinceLastPong;

    public PhotonPipelineMetadata(
            long captureTimestampMicros,
            long publishTimestampMicros,
            long sequenceID,
            long timeSinceLastPong) {
        this.captureTimestampMicros = captureTimestampMicros;
        this.publishTimestampMicros = publishTimestampMicros;
        this.sequenceID = sequenceID;
        this.timeSinceLastPong = timeSinceLastPong;
    }

    public PhotonPipelineMetadata() {
        this(-1, -1, -1, Long.MAX_VALUE);
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

    @Override
    public String toString() {
        return "PhotonPipelineMetadata [captureTimestampMicros="
                + captureTimestampMicros
                + ", publishTimestampMicros="
                + publishTimestampMicros
                + ", sequenceID="
                + sequenceID
                + ", timeSinceLastPong="
                + timeSinceLastPong
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (captureTimestampMicros ^ (captureTimestampMicros >>> 32));
        result = prime * result + (int) (publishTimestampMicros ^ (publishTimestampMicros >>> 32));
        result = prime * result + (int) (sequenceID ^ (sequenceID >>> 32));
        result = prime * result + (int) (timeSinceLastPong ^ (timeSinceLastPong >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonPipelineMetadata other = (PhotonPipelineMetadata) obj;
        if (captureTimestampMicros != other.captureTimestampMicros) return false;
        if (publishTimestampMicros != other.publishTimestampMicros) return false;
        if (sequenceID != other.sequenceID) return false;
        if (timeSinceLastPong != other.timeSinceLastPong) return false;
        return true;
    }

    public static final PhotonPipelineMetadataSerde photonStruct = new PhotonPipelineMetadataSerde();

    @Override
    public PacketSerde<PhotonPipelineMetadata> getSerde() {
        return photonStruct;
    }
}

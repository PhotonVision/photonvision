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

package org.photonvision.targeting;

import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.PhotonPipelineMetadataSerde;
import org.photonvision.targeting.serde.PhotonStructSerializable;

public class PhotonPipelineMetadata implements PhotonStructSerializable<PhotonPipelineMetadata> {
    // Image capture and NT publish timestamp, in nanoseconds
    // The timebase is wpi::nt::Now on the time sync server
    public long captureTimestampNanos;
    public long publishTimestampNanos;

    // Mirror of the heartbeat entry -- monotonically increasing
    public long sequenceID;

    // Time from last Time Sync Pong received and the construction of this metadata, in nS
    public long timeSinceLastPong;

    public PhotonPipelineMetadata(
            long captureTimestampNanos,
            long publishTimestampNanos,
            long sequenceID,
            long timeSinceLastPong) {
        this.captureTimestampNanos = captureTimestampNanos;
        this.publishTimestampNanos = publishTimestampNanos;
        this.sequenceID = sequenceID;
        this.timeSinceLastPong = timeSinceLastPong;
    }

    public PhotonPipelineMetadata() {
        this(-1, -1, -1, Long.MAX_VALUE);
    }

    /**
     * Returns the time between image capture and publish to NT
     *
     * @return The time in milliseconds
     */
    public double getLatencyMillis() {
        return (publishTimestampNanos - captureTimestampNanos) / 1e6;
    }

    /**
     * The time that this image was captured, in the coprocessor's time base.
     *
     * @return The time in nanoseconds
     */
    public long getCaptureTimestampNanos() {
        return captureTimestampNanos;
    }

    /**
     * The time that this result was published to NT, in the coprocessor's time base.
     *
     * @return The time in nanoseconds
     */
    public long getPublishTimestampNanos() {
        return publishTimestampNanos;
    }

    /**
     * The number of non-empty frames processed by this camera since boot. Useful to checking if a
     * camera is alive.
     *
     * @return The number of non-empty frames
     */
    public long getSequenceID() {
        return sequenceID;
    }

    @Override
    public String toString() {
        return "PhotonPipelineMetadata [captureTimestampNanos="
                + captureTimestampNanos
                + ", publishTimestampNanos="
                + publishTimestampNanos
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
        result = prime * result + (int) (captureTimestampNanos ^ (captureTimestampNanos >>> 32));
        result = prime * result + (int) (publishTimestampNanos ^ (publishTimestampNanos >>> 32));
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
        if (captureTimestampNanos != other.captureTimestampNanos) return false;
        if (publishTimestampNanos != other.publishTimestampNanos) return false;
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

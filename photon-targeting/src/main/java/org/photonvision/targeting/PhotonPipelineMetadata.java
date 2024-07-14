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

    @Override
    public String toString() {
        return "PhotonPipelineMetadata [sequenceID="
                + sequenceID
                + ", captureTimestampMicros="
                + captureTimestampMicros
                + ", publishTimestampMicros="
                + publishTimestampMicros
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (sequenceID ^ (sequenceID >>> 32));
        result = prime * result + (int) (captureTimestampMicros ^ (captureTimestampMicros >>> 32));
        result = prime * result + (int) (publishTimestampMicros ^ (publishTimestampMicros >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonPipelineMetadata other = (PhotonPipelineMetadata) obj;
        if (sequenceID != other.sequenceID) return false;
        if (captureTimestampMicros != other.captureTimestampMicros) return false;
        if (publishTimestampMicros != other.publishTimestampMicros) return false;
        return true;
    }

    public static final PhotonPipelineMetadataSerde photonStruct = new PhotonPipelineMetadataSerde();

    @Override
    public PacketSerde<PhotonPipelineMetadata> getSerde() {
        return photonStruct;
    }
}

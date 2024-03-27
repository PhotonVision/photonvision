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

import edu.wpi.first.util.protobuf.ProtobufSerializable;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.targeting.proto.PhotonPipelineResultProto;

/** Represents a pipeline result from a PhotonCamera. */
public class PhotonPipelineResult implements ProtobufSerializable {
    private static boolean HAS_WARNED = false;

    // Image capture and NT publish timestamp, in microseconds and in the coprocessor timebase. As
    // reported by WPIUtilJNI::now.
    private long captureTimestampMicros = -1;
    private long publishTimestampMicros = -1;

    // Mirror of the heartbeat entry -- monotonically increasing
    private long sequenceID = -1;

    // Targets to store.
    public final List<PhotonTrackedTarget> targets = new ArrayList<>();

    // Multi-tag result
    private MultiTargetPNPResult multiTagResult = new MultiTargetPNPResult();

    // Since we don't trust NT time sync, keep track of when we got this packet into robot code
    private long ntRecieveTimestampMicros;

    /** Constructs an empty pipeline result. */
    public PhotonPipelineResult() {}

    /**
     * Constructs a pipeline result.
     *
     * @param sequenceID The number of frames processed by this camera since boot
     * @param captureTimestamp The time, in uS in the coprocessor's timebase, that the coprocessor
     *     captured the image this result contains the targeting info of
     * @param publishTimestamp The time, in uS in the coprocessor's timebase, that the coprocessor
     *     published targeting info
     * @param targets The list of targets identified by the pipeline.
     */
    public PhotonPipelineResult(
            long sequenceID,
            long captureTimestamp,
            long publishTimestamp,
            List<PhotonTrackedTarget> targets) {
        this.captureTimestampMicros = captureTimestamp;
        this.publishTimestampMicros = publishTimestamp;
        this.sequenceID = sequenceID;
        this.targets.addAll(targets);
    }

    /**
     * Constructs a pipeline result.
     *
     * @param sequenceID The number of frames processed by this camera since boot
     * @param captureTimestamp The time, in uS in the coprocessor's timebase, that the coprocessor
     *     captured the image this result contains the targeting info of
     * @param publishTimestamp The time, in uS in the coprocessor's timebase, that the coprocessor
     *     published targeting info
     * @param targets The list of targets identified by the pipeline.
     * @param result Result from multi-target PNP.
     */
    public PhotonPipelineResult(
            long sequenceID,
            long captureTimestamp,
            long publishTimestamp,
            List<PhotonTrackedTarget> targets,
            MultiTargetPNPResult result) {
        this.captureTimestampMicros = captureTimestamp;
        this.publishTimestampMicros = publishTimestamp;
        this.sequenceID = sequenceID;
        this.targets.addAll(targets);
        this.multiTagResult = result;
    }

    /**
     * Returns the size of the packet needed to store this pipeline result.
     *
     * @return The size of the packet needed to store this pipeline result.
     */
    public int getPacketSize() {
        return Double.BYTES // latency
                + 1 // target count
                + targets.size() * PhotonTrackedTarget.serde.getMaxByteSize()
                + MultiTargetPNPResult.serde.getMaxByteSize();
    }

    /**
     * Returns the best target in this pipeline result. If there are no targets, this method will
     * return null. The best target is determined by the target sort mode in the PhotonVision UI.
     *
     * @return The best target of the pipeline result.
     */
    public PhotonTrackedTarget getBestTarget() {
        if (!hasTargets() && !HAS_WARNED) {
            String errStr =
                    "This PhotonPipelineResult object has no targets associated with it! Please check hasTargets() "
                            + "before calling this method. For more information, please review the PhotonLib "
                            + "documentation at https://docs.photonvision.org";
            System.err.println(errStr);
            new Exception().printStackTrace();
            HAS_WARNED = true;
        }
        return hasTargets() ? targets.get(0) : null;
    }

    /** Returns the time between image capture and publish to NT */
    public double getLatencyMillis() {
        return (publishTimestampMicros - captureTimestampMicros) / 1e3;
    }

    /**
     * Returns the estimated time the frame was taken, in the recieved system's time base. This is
     * calculated as (NT recieve time (robot base) - (publish timestamp, coproc timebase - capture
     * timestamp, coproc timebase))
     *
     * @return The timestamp in seconds
     */
    public double getTimestampSeconds() {
        return (ntRecieveTimestampMicros - (publishTimestampMicros - captureTimestampMicros)) / 1e6;
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

    /** The time that the robot recieved this result, in the FPGA timebase. */
    public long getNtRecieveTimestampMicros() {
        return ntRecieveTimestampMicros;
    }

    /** Sets the FPGA timestamp this result was recieved by robot code */
    public void setRecieveTimestampMicros(long timestampMicros) {
        this.ntRecieveTimestampMicros = timestampMicros;
    }

    /**
     * Returns whether the pipeline has targets.
     *
     * @return Whether the pipeline has targets.
     */
    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    /**
     * Returns a copy of the vector of targets.
     *
     * @return A copy of the vector of targets.
     */
    public List<PhotonTrackedTarget> getTargets() {
        return new ArrayList<>(targets);
    }

    /**
     * Return the latest multi-target result. Be sure to check
     * getMultiTagResult().estimatedPose.isPresent before using the pose estimate!
     */
    public MultiTargetPNPResult getMultiTagResult() {
        return multiTagResult;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (captureTimestampMicros ^ (captureTimestampMicros >>> 32));
        long temp;
        temp = Double.doubleToLongBits(publishTimestampMicros);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (int) (sequenceID ^ (sequenceID >>> 32));
        result = prime * result + ((targets == null) ? 0 : targets.hashCode());
        result = prime * result + ((multiTagResult == null) ? 0 : multiTagResult.hashCode());
        result = prime * result + (int) (ntRecieveTimestampMicros ^ (ntRecieveTimestampMicros >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonPipelineResult other = (PhotonPipelineResult) obj;
        if (captureTimestampMicros != other.captureTimestampMicros) return false;
        if (Double.doubleToLongBits(publishTimestampMicros)
                != Double.doubleToLongBits(other.publishTimestampMicros)) return false;
        if (sequenceID != other.sequenceID) return false;
        if (targets == null) {
            if (other.targets != null) return false;
        } else if (!targets.equals(other.targets)) return false;
        if (multiTagResult == null) {
            if (other.multiTagResult != null) return false;
        } else if (!multiTagResult.equals(other.multiTagResult)) return false;
        if (ntRecieveTimestampMicros != other.ntRecieveTimestampMicros) return false;
        return true;
    }

    @Override
    public String toString() {
        return "PhotonPipelineResult [captureTimestamp="
                + captureTimestampMicros
                + ", publishTimestamp="
                + publishTimestampMicros
                + ", sequenceID="
                + sequenceID
                + ", targets="
                + targets
                + ", multiTagResult="
                + multiTagResult
                + ", ntRecieveTimestamp="
                + ntRecieveTimestampMicros
                + "]";
    }

    public static final class APacketSerde implements PacketSerde<PhotonPipelineResult> {
        @Override
        public int getMaxByteSize() {
            // This uses dynamic packets so it doesn't matter
            return -1;
        }

        @Override
        public void pack(Packet packet, PhotonPipelineResult value) {
            packet.encode(value.sequenceID);
            packet.encode(value.captureTimestampMicros);
            packet.encode(value.publishTimestampMicros);
            packet.encode((byte) value.targets.size());
            for (var target : value.targets) PhotonTrackedTarget.serde.pack(packet, target);
            MultiTargetPNPResult.serde.pack(packet, value.multiTagResult);
        }

        @Override
        public PhotonPipelineResult unpack(Packet packet) {
            var seq = packet.decodeLong();
            var cap = packet.decodeLong();
            var pub = packet.decodeLong();
            var len = packet.decodeByte();
            var targets = new ArrayList<PhotonTrackedTarget>(len);
            for (int i = 0; i < len; i++) {
                targets.add(PhotonTrackedTarget.serde.unpack(packet));
            }
            var result = MultiTargetPNPResult.serde.unpack(packet);

            return new PhotonPipelineResult(seq, cap, pub, targets, result);
        }
    }

    public static final APacketSerde serde = new APacketSerde();
    public static final PhotonPipelineResultProto proto = new PhotonPipelineResultProto();
}

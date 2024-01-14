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
import org.photonvision.targeting.proto.MultiTargetPNPResultProto;

public class MultiTargetPNPResult implements ProtobufSerializable {
    // Seeing 32 apriltags at once seems like a sane limit
    private static final int MAX_IDS = 32;

    public PNPResult estimatedPose = new PNPResult();
    public List<Integer> fiducialIDsUsed = List.of();

    public MultiTargetPNPResult() {}

    public MultiTargetPNPResult(PNPResult results, List<Integer> ids) {
        estimatedPose = results;
        fiducialIDsUsed = ids;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((estimatedPose == null) ? 0 : estimatedPose.hashCode());
        result = prime * result + ((fiducialIDsUsed == null) ? 0 : fiducialIDsUsed.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MultiTargetPNPResult other = (MultiTargetPNPResult) obj;
        if (estimatedPose == null) {
            if (other.estimatedPose != null) return false;
        } else if (!estimatedPose.equals(other.estimatedPose)) return false;
        if (fiducialIDsUsed == null) {
            if (other.fiducialIDsUsed != null) return false;
        } else if (!fiducialIDsUsed.equals(other.fiducialIDsUsed)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MultiTargetPNPResult [estimatedPose="
                + estimatedPose
                + ", fiducialIDsUsed="
                + fiducialIDsUsed
                + "]";
    }

    public static final class APacketSerde implements PacketSerde<MultiTargetPNPResult> {
        @Override
        public int getMaxByteSize() {
            // PNPResult + MAX_IDS possible targets (arbitrary upper limit that should never be hit,
            // ideally)
            return PNPResult.serde.getMaxByteSize() + (Short.BYTES * MAX_IDS);
        }

        @Override
        public void pack(Packet packet, MultiTargetPNPResult result) {
            PNPResult.serde.pack(packet, result.estimatedPose);

            for (int i = 0; i < MAX_IDS; i++) {
                if (i < result.fiducialIDsUsed.size()) {
                    packet.encode((short) result.fiducialIDsUsed.get(i).byteValue());
                } else {
                    packet.encode((short) -1);
                }
            }
        }

        @Override
        public MultiTargetPNPResult unpack(Packet packet) {
            var results = PNPResult.serde.unpack(packet);
            var ids = new ArrayList<Integer>(MAX_IDS);
            for (int i = 0; i < MAX_IDS; i++) {
                int targetId = packet.decodeShort();
                if (targetId > -1) ids.add(targetId);
            }
            return new MultiTargetPNPResult(results, ids);
        }
    }

    public static final APacketSerde serde = new APacketSerde();
    public static final MultiTargetPNPResultProto proto = new MultiTargetPNPResultProto();
}

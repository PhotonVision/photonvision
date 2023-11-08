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

import edu.wpi.first.util.protobuf.Protobuf;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.proto.PhotonTypes.ProtobufMultiTargetPNPResults;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedInt;

public class MultiTargetPNPResults {
    public PNPResults estimatedPose = new PNPResults();
    public List<Integer> fiducialIDsUsed = List.of();

    public MultiTargetPNPResults() {}

    public MultiTargetPNPResults(PNPResults results, List<Integer> ids) {
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
        MultiTargetPNPResults other = (MultiTargetPNPResults) obj;
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
        return "MultiTargetPNPResults [estimatedPose="
                + estimatedPose
                + ", fiducialIDsUsed="
                + fiducialIDsUsed
                + "]";
    }

    public static final class AProto
            implements Protobuf<MultiTargetPNPResults, ProtobufMultiTargetPNPResults> {
        @Override
        public Class<MultiTargetPNPResults> getTypeClass() {
            return MultiTargetPNPResults.class;
        }

        @Override
        public Descriptor getDescriptor() {
            return ProtobufMultiTargetPNPResults.getDescriptor();
        }

        @Override
        public ProtobufMultiTargetPNPResults createMessage() {
            return ProtobufMultiTargetPNPResults.newInstance();
        }

        @Override
        public MultiTargetPNPResults unpack(ProtobufMultiTargetPNPResults msg) {
            return new MultiTargetPNPResults(
                    PNPResults.proto.unpack(msg.getEstimatedPose()),
                    // TODO better way of doing this
                    Arrays.stream(msg.getFiducialIdsUsed().array()).boxed().collect(Collectors.toList()));
        }

        @Override
        public void pack(ProtobufMultiTargetPNPResults msg, MultiTargetPNPResults value) {
            PNPResults.proto.pack(msg.getMutableEstimatedPose(), value.estimatedPose);

            RepeatedInt idsUsed = msg.getMutableFiducialIdsUsed().reserve(value.fiducialIDsUsed.size());
            for (int i = 0; i < value.fiducialIDsUsed.size(); i++) {
                idsUsed.add(value.fiducialIDsUsed.get(i));
            }
        }
    }

    public static final AProto proto = new AProto();
}

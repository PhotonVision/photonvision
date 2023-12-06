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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.util.protobuf.Protobuf;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.proto.Photon.ProtobufMultiTargetPNPResult;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedInt;

public class MultiTargetPNPResult extends PNPResult {
    public List<Integer> fiducialIDsUsed;

    public MultiTargetPNPResult(
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr,
            List<Integer> ids) {
        super(best, alt, ambiguity, bestReprojErr, altReprojErr);
        fiducialIDsUsed = ids;
    }

    public MultiTargetPNPResult(PNPResult results, List<Integer> ids) {
        this(
                results.best,
                results.alt,
                results.bestReprojErr,
                results.altReprojErr,
                results.ambiguity,
                ids);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + ((fiducialIDsUsed == null) ? 0 : fiducialIDsUsed.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MultiTargetPNPResult other = (MultiTargetPNPResult) obj;
        if (!super.equals(other)) return false;
        if (!fiducialIDsUsed.equals(other.fiducialIDsUsed)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MultiTargetPNPResult [estimatedPose="
                + super.toString()
                + ", fiducialIDsUsed="
                + fiducialIDsUsed
                + "]";
    }

    public static final class AProto
            implements Protobuf<MultiTargetPNPResult, ProtobufMultiTargetPNPResult> {
        @Override
        public Class<MultiTargetPNPResult> getTypeClass() {
            return MultiTargetPNPResult.class;
        }

        @Override
        public Descriptor getDescriptor() {
            return ProtobufMultiTargetPNPResult.getDescriptor();
        }

        @Override
        public Protobuf<?, ?>[] getNested() {
            return new Protobuf<?, ?>[] {Transform3d.proto};
        }

        @Override
        public ProtobufMultiTargetPNPResult createMessage() {
            return ProtobufMultiTargetPNPResult.newInstance();
        }

        @Override
        public MultiTargetPNPResult unpack(ProtobufMultiTargetPNPResult msg) {
            return new MultiTargetPNPResult(
                    Transform3d.proto.unpack(msg.getBest()),
                    Transform3d.proto.unpack(msg.getAlt()),
                    msg.getAmbiguity(),
                    msg.getBestReprojErr(),
                    msg.getAltReprojErr(),
                    // TODO better way of doing this
                    Arrays.stream(msg.getFiducialIdsUsed().array()).boxed().collect(Collectors.toList()));
        }

        @Override
        public void pack(ProtobufMultiTargetPNPResult msg, MultiTargetPNPResult value) {
            Transform3d.proto.pack(msg.getMutableBest(), value.best);
            Transform3d.proto.pack(msg.getMutableAlt(), value.alt);
            msg.setAmbiguity(value.ambiguity)
                    .setBestReprojErr(value.bestReprojErr)
                    .setAltReprojErr(value.altReprojErr);

            RepeatedInt idsUsed = msg.getMutableFiducialIdsUsed().reserve(value.fiducialIDsUsed.size());
            for (int i = 0; i < value.fiducialIDsUsed.size(); i++) {
                idsUsed.add(value.fiducialIDsUsed.get(i));
            }
        }
    }

    public static final AProto proto = new AProto();
}

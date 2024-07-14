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
import java.util.List;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.MultiTargetPNPResultSerde;
import org.photonvision.targeting.proto.MultiTargetPNPResultProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

public class MultiTargetPNPResult
        implements ProtobufSerializable, PhotonStructSerializable<MultiTargetPNPResult> {
    // Seeing 32 apriltags at once seems like a sane limit
    private static final int MAX_IDS = 32;

    public PnpResult estimatedPose = new PnpResult();
    public List<Short> fiducialIDsUsed = List.of();

    public MultiTargetPNPResult() {}

    public MultiTargetPNPResult(PnpResult results, List<Short> ids) {
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

    public static final MultiTargetPNPResultProto proto = new MultiTargetPNPResultProto();

    // tODO!
    public static final MultiTargetPNPResultSerde photonStruct = new MultiTargetPNPResultSerde();

    @Override
    public PacketSerde<MultiTargetPNPResult> getSerde() {
        return photonStruct;
    }
}

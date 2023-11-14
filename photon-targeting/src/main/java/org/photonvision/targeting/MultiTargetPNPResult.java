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

import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;

public class MultiTargetPNPResult {
    // Seeing 32 apriltags at once seems like a sane limit
    private static final int MAX_IDS = 32;
    // pnpresult + MAX_IDS possible targets (arbitrary upper limit that should never be hit, ideally)
    public static final int PACK_SIZE_BYTES = PNPResult.PACK_SIZE_BYTES + (Short.BYTES * MAX_IDS);

    public PNPResult estimatedPose = new PNPResult();
    public List<Integer> fiducialIDsUsed = List.of();

    public MultiTargetPNPResult() {}

    public MultiTargetPNPResult(PNPResult results, List<Integer> ids) {
        estimatedPose = results;
        fiducialIDsUsed = ids;
    }

    public static MultiTargetPNPResult createFromPacket(Packet packet) {
        var results = PNPResult.createFromPacket(packet);
        var ids = new ArrayList<Integer>(MAX_IDS);
        for (int i = 0; i < MAX_IDS; i++) {
            int targetId = (int) packet.decodeShort();
            if (targetId > -1) ids.add(targetId);
        }
        return new MultiTargetPNPResult(results, ids);
    }

    public void populatePacket(Packet packet) {
        estimatedPose.populatePacket(packet);
        for (int i = 0; i < MAX_IDS; i++) {
            if (i < fiducialIDsUsed.size()) {
                packet.encode((short) fiducialIDsUsed.get(i).byteValue());
            } else {
                packet.encode((short) -1);
            }
        }
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
}

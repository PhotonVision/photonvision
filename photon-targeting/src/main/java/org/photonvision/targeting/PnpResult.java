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
import edu.wpi.first.util.protobuf.ProtobufSerializable;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.PnpResultSerde;
import org.photonvision.targeting.proto.PNPResultProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

/**
 * The best estimated transformation from solvePnP, and possibly an alternate transformation
 * depending on the solvePNP method. If an alternate solution is present, the ambiguity value
 * represents the ratio of reprojection error in the best solution to the alternate (best /
 * alternate).
 *
 * <p>Note that the coordinate frame of these transforms depends on the implementing solvePnP
 * method.
 */
public class PnpResult implements ProtobufSerializable, PhotonStructSerializable<PnpResult> {
    /**
     * The best-fit transform. The coordinate frame of this transform depends on the method which gave
     * this result.
     */
    public Transform3d best;

    /** Reprojection error of the best solution, in pixels */
    public double bestReprojErr;

    /**
     * Alternate, ambiguous solution from solvepnp. If no alternate solution is found, this is equal
     * to the best solution.
     */
    public Transform3d alt;

    /** If no alternate solution is found, this is bestReprojErr */
    public double altReprojErr;

    /** If no alternate solution is found, this is 0 */
    public double ambiguity;

    /** An empty (invalid) result. */
    public PnpResult() {
        this.best = new Transform3d();
        this.alt = new Transform3d();
        this.ambiguity = 0;
        this.bestReprojErr = 0;
        this.altReprojErr = 0;
    }

    public PnpResult(Transform3d best, double bestReprojErr) {
        this(best, best, 0, bestReprojErr, bestReprojErr);
    }

    public PnpResult(
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr) {
        this.best = best;
        this.alt = alt;
        this.ambiguity = ambiguity;
        this.bestReprojErr = bestReprojErr;
        this.altReprojErr = altReprojErr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((best == null) ? 0 : best.hashCode());
        long temp;
        temp = Double.doubleToLongBits(bestReprojErr);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((alt == null) ? 0 : alt.hashCode());
        temp = Double.doubleToLongBits(altReprojErr);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ambiguity);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PnpResult other = (PnpResult) obj;
        if (best == null) {
            if (other.best != null) return false;
        } else if (!best.equals(other.best)) return false;
        if (Double.doubleToLongBits(bestReprojErr) != Double.doubleToLongBits(other.bestReprojErr))
            return false;
        if (alt == null) {
            if (other.alt != null) return false;
        } else if (!alt.equals(other.alt)) return false;
        if (Double.doubleToLongBits(altReprojErr) != Double.doubleToLongBits(other.altReprojErr))
            return false;
        if (Double.doubleToLongBits(ambiguity) != Double.doubleToLongBits(other.ambiguity))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PnpResult [best="
                + best
                + ", bestReprojErr="
                + bestReprojErr
                + ", alt="
                + alt
                + ", altReprojErr="
                + altReprojErr
                + ", ambiguity="
                + ambiguity
                + "]";
    }

    public static final PNPResultProto proto = new PNPResultProto();
    public static final PnpResultSerde photonStruct = new PnpResultSerde();

    @Override
    public PacketSerde<PnpResult> getSerde() {
        return photonStruct;
    }
}

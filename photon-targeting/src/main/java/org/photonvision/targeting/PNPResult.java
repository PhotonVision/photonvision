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
import org.photonvision.proto.PhotonTypes.ProtobufPNPResult;
import us.hebi.quickbuf.Descriptors.Descriptor;

/**
 * The best estimated transformation from solvePnP, and possibly an alternate transformation
 * depending on the solvePNP method. If an alternate solution is present, the ambiguity value
 * represents the ratio of reprojection error in the best solution to the alternate (best /
 * alternate).
 *
 * <p>Note that the coordinate frame of these transforms depends on the implementing solvePnP
 * method.
 */
public class PNPResult {
    /**
     * If this result is valid. A false value indicates there was an error in estimation, and this
     * result should not be used.
     */
    public final boolean isPresent;

    /**
     * The best-fit transform. The coordinate frame of this transform depends on the method which gave
     * this result.
     */
    public final Transform3d best;

    /** Reprojection error of the best solution, in pixels */
    public final double bestReprojErr;

    /**
     * Alternate, ambiguous solution from solvepnp. If no alternate solution is found, this is equal
     * to the best solution.
     */
    public final Transform3d alt;

    /** If no alternate solution is found, this is bestReprojErr */
    public final double altReprojErr;

    /** If no alternate solution is found, this is 0 */
    public final double ambiguity;

    /** An empty (invalid) result. */
    public PNPResult() {
        this.isPresent = false;
        this.best = new Transform3d();
        this.alt = new Transform3d();
        this.ambiguity = 0;
        this.bestReprojErr = 0;
        this.altReprojErr = 0;
    }

    public PNPResult(Transform3d best, double bestReprojErr) {
        this(best, best, 0, bestReprojErr, bestReprojErr);
    }

    public PNPResult(
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr) {
        this.isPresent = true;
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
        result = prime * result + (isPresent ? 1231 : 1237);
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
        PNPResult other = (PNPResult) obj;
        if (isPresent != other.isPresent) return false;
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
        return "PNPResult [isPresent="
                + isPresent
                + ", best="
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

    public static final class AProto implements Protobuf<PNPResult, ProtobufPNPResult> {
        @Override
        public Class<PNPResult> getTypeClass() {
            return PNPResult.class;
        }

        @Override
        public Descriptor getDescriptor() {
            return ProtobufPNPResult.getDescriptor();
        }

        @Override
        public Protobuf<?, ?>[] getNested() {
            return new Protobuf<?, ?>[] {Transform3d.proto};
        }

        @Override
        public ProtobufPNPResult createMessage() {
            return ProtobufPNPResult.newInstance();
        }

        @Override
        public PNPResult unpack(ProtobufPNPResult msg) {
            if (!msg.getIsPresent()) {
                return new PNPResult();
            }

            return new PNPResult(
                    Transform3d.proto.unpack(msg.getBest()),
                    Transform3d.proto.unpack(msg.getAlt()),
                    msg.getAmbiguity(),
                    msg.getBestReprojErr(),
                    msg.getAltReprojErr());
        }

        @Override
        public void pack(ProtobufPNPResult msg, PNPResult value) {
            Transform3d.proto.pack(msg.getMutableBest(), value.best);
            Transform3d.proto.pack(msg.getMutableAlt(), value.alt);
            msg.setAmbiguity(value.ambiguity)
                    .setBestReprojErr(value.bestReprojErr)
                    .setAltReprojErr(value.altReprojErr)
                    .setIsPresent(value.isPresent);
        }
    }

    public static final AProto proto = new AProto();
}

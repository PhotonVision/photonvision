/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

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

import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.utils.PacketUtils;

import edu.wpi.first.math.geometry.Transform3d;

/**
 * The best estimated transformation from solvePnP, and possibly an alternate
 * transformation
 * depending on the solvePNP method. If an alternate solution is present, the
 * ambiguity value
 * represents the ratio of reprojection error in the best solution to the
 * alternate (best /
 * alternate).
 *
 * <p>
 * Note that the coordinate frame of these transforms depends on the
 * implementing solvePnP
 * method.
 */
public class PNPResults {
    // Imitate optional by having a valid check
    public final boolean isPresent;
    
    // Best transform. Coordinate frame depends on where this result comes from
    public final Transform3d best;
    // Reprojection error of this solution, in pixels
    public final double bestReprojErr;

    /**
     * Alternate, ambiguous solution from solvepnp. If no alternate solution is
     * found, this is equal
     * to the best solution.
     */
    public final Transform3d alt;
    /** If no alternate solution is found, this is bestReprojErr */
    public final double altReprojErr;
    /** If no alternate solution is found, this is 0 */
    public final double ambiguity;

    public PNPResults() {
        this(false, new Transform3d(), new Transform3d(), -1, 0, 0);
    }

    public PNPResults(Transform3d best, double bestReprojErr) {
        this(best, best, 0, bestReprojErr, bestReprojErr);
    }

    public PNPResults(
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr) {
        this(true, best, alt, ambiguity, bestReprojErr, altReprojErr);
    }

    public PNPResults(
            boolean isPresent,
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr) {
        this.isPresent = isPresent;
        this.best = best;
        this.alt = alt;
        this.ambiguity = ambiguity;
        this.bestReprojErr = bestReprojErr;
        this.altReprojErr = altReprojErr;
    }

    public static final int PACK_SIZE_BYTES = 1 + (Double.BYTES * 7 * 2) + (Double.BYTES * 3);

    public static PNPResults createFromPacket(Packet packet) {
        var present = packet.decodeBoolean();
        var best = PacketUtils.decodeTransform(packet);
        var alt = PacketUtils.decodeTransform(packet);
        var bestEr = packet.decodeDouble();
        var altEr = packet.decodeDouble();
        var ambiguity = packet.decodeDouble();
        return new PNPResults(present, best, alt, ambiguity, bestEr, altEr);
    }

    public Packet populatePacket(Packet packet) {
        packet.encode(isPresent);
        PacketUtils.encodeTransform(packet, best);
        PacketUtils.encodeTransform(packet, alt);
        packet.encode(bestReprojErr);
        packet.encode(altReprojErr);
        packet.encode(ambiguity);
        return packet;
    }
}

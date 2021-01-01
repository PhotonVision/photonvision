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

package org.photonvision.vision.pipeline.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.vision.target.TrackedTarget;

public class SimplePipelineResult {

    private double latencyMillis;
    private boolean hasTargets;
    public final List<SimpleTrackedTarget> targets = new ArrayList<>();

    public SimplePipelineResult() {}

    public SimplePipelineResult(
            double latencyMillis, boolean hasTargets, List<SimpleTrackedTarget> targets) {
        this.latencyMillis = latencyMillis;
        this.hasTargets = hasTargets;
        this.targets.addAll(targets);
    }

    public SimplePipelineResult(CVPipelineResult r) {
        this(r.processingMillis, r.hasTargets(), simpleFromTrackedTargets(r.targets));
    }

    /**
    * Returns the size of the packet needed to store this pipeline result.
    *
    * @return The size of the packet needed to store this pipeline result.
    */
    public int getPacketSize() {
        return targets.size() * SimpleTrackedTarget.PACK_SIZE_BYTES + 8 + 2;
    }

    public double getLatencyMillis() {
        return latencyMillis;
    }

    public boolean hasTargets() {
        return hasTargets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimplePipelineResult that = (SimplePipelineResult) o;
        boolean latencyMatch = Double.compare(that.latencyMillis, latencyMillis) == 0;
        boolean hasTargetsMatch = that.hasTargets == hasTargets;
        boolean targetsMatch = that.targets.equals(targets);
        return latencyMatch && hasTargetsMatch && targetsMatch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latencyMillis, hasTargets, targets);
    }

    /**
    * Populates the fields of the pipeline result from the packet.
    *
    * @param packet The incoming packet.
    * @return The incoming packet.
    */
    public Packet createFromPacket(Packet packet) {
        // Decode latency, existence of targets, and number of targets.
        latencyMillis = packet.decodeDouble();
        hasTargets = packet.decodeBoolean();
        byte targetCount = packet.decodeByte();

        targets.clear();

        // Decode the information of each target.
        for (int i = 0; i < (int) targetCount; ++i) {
            var target = new SimpleTrackedTarget();
            target.createFromPacket(packet);
            targets.add(target);
        }

        return packet;
    }

    /**
    * Populates the outgoing packet with information from this pipeline result.
    *
    * @param packet The outgoing packet.
    * @return The outgoing packet.
    */
    public Packet populatePacket(Packet packet) {
        // Encode latency, existence of targets, and number of targets.
        packet.encode(latencyMillis);
        packet.encode(hasTargets);
        packet.encode((byte) targets.size());

        // Encode the information of each target.
        for (var target : targets) target.populatePacket(packet);

        // Return the packet.
        return packet;
    }

    private static List<SimpleTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<SimpleTrackedTarget>();
        for (var t : targets) {
            ret.add(new SimpleTrackedTarget(t));
        }
        return ret;
    }
}

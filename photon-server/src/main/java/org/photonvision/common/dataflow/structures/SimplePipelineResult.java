/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.dataflow.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class SimplePipelineResult extends BytePackable {

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

    public SimplePipelineResult(CVPipelineResult origResult) {
        update(origResult);
    }

    public void update(CVPipelineResult origResult) {
        latencyMillis = origResult.getLatencyMillis();
        hasTargets = origResult.hasTargets();
        targets.clear();
        for (var origTarget : origResult.targets) {
            targets.add(new SimpleTrackedTarget(origTarget));
        }
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

    @Override
    public byte[] toByteArray() {
        bufferPosition = 0;
        int bufferSize =
                8 + 1 + 1 + (targets.size() * 48); // latencyMillis + hasTargets + targetCount + targets
        var buff = new byte[bufferSize];

        bufferData(latencyMillis, buff);
        bufferData(hasTargets, buff);
        bufferData((byte) targets.size(), buff);
        for (var target : targets) {
            bufferData(target.toByteArray(), buff);
        }

        return buff;
    }

    @Override
    public void fromByteArray(byte[] src) {
        bufferPosition = 0;

        latencyMillis = unbufferDouble(src);
        hasTargets = unbufferBoolean(src);
        byte targetCount = unbufferByte(src);

        targets.clear();
        for (int i = 0; i < targetCount; i++) {
            var target = new SimpleTrackedTarget();
            target.fromByteArray(unbufferBytes(src, 48));
            bufferPosition += 48;
            targets.add(target);
        }
    }
}

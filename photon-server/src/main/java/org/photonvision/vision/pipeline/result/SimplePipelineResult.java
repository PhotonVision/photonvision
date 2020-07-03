package org.photonvision.vision.pipeline.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.photonvision.vision.target.TrackedTarget;

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

    public SimplePipelineResult(CVPipelineResult r) {
        this(r.processingMillis, r.hasTargets(), simpleFromTrackedTargets(r.targets));
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

    private static List<SimpleTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<SimpleTrackedTarget>();
        for (var t : targets) {
            ret.add(new SimpleTrackedTarget(t));
        }
        return ret;
    }
}

package org.photonvision.common.vision.pipeline;

import org.photonvision.common.vision.frame.Frame;
import org.photonvision.common.vision.opencv.Releasable;
import org.photonvision.common.vision.target.TrackedTarget;

import java.util.List;

public class CVPipelineResult implements Releasable {
    private double latencyMillis;
    public final double processingMillis;
    public final List<TrackedTarget> targets;
    public final Frame outputFrame;

    public CVPipelineResult(double processingMillis, List<TrackedTarget> targets, Frame outputFrame) {
        this.processingMillis = processingMillis;
        this.targets = targets;

        this.outputFrame = Frame.copyFrom(outputFrame);
    }

    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    public void release() {
        for (TrackedTarget tt : targets) {
            tt.release();
        }
        outputFrame.release();
    }

    public double getLatencyMillis() {
        return latencyMillis;
    }

    protected void setLatencyMillis(double latencyMillis) {
        this.latencyMillis = latencyMillis;
    }
}

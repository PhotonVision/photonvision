package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.opencv.Releasable;
import com.chameleonvision.common.vision.target.TrackedTarget;
import java.util.List;

public class CVPipelineResult implements Releasable {
    private double latencyMillis;
    public final double processingMillis;
    public final List<TrackedTarget> targets;
    public final Frame outputFrame;

    public CVPipelineResult(double processingMillis, List<TrackedTarget> targets, Frame outputFrame) {
        this.processingMillis = processingMillis;
        this.targets = targets;

        // TODO: is this the best way to go about this?
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

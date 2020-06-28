package org.photonvision.vision.pipeline;

import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;

public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected S settings;

    protected abstract void setPipeParams(FrameStaticProperties frameStaticProperties, S settings);

    protected abstract R process(Frame frame, S settings);

    public S getSettings() {
        return settings;
    }

    public R run(Frame frame) {
        long pipelineStartNanos = System.nanoTime();

        if (settings == null) {
            throw new RuntimeException("No settings provided for pipeline!");
        }
        setPipeParams(frame.frameStaticProperties, settings);

        R result = process(frame, settings);

        result.setLatencyMillis(MathUtils.nanosToMillis(System.nanoTime() - pipelineStartNanos));

        return result;
    }
}

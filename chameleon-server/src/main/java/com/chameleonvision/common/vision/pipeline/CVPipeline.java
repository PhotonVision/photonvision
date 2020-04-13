package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.util.math.MathUtils;
import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;

public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {

    protected abstract void setPipeParams(S settings, FrameStaticProperties frameStaticProperties);

    protected abstract R process(Frame frame, S settings);

    public R run(Frame frame, S settings) {
        long pipelineStartNanos = System.nanoTime();

        setPipeParams(settings, frame.frameStaticProperties);

        R result = process(frame, settings);

        result.setLatencyMillis(MathUtils.nanosToMillis(System.nanoTime() - pipelineStartNanos));

        return result;
    }
}

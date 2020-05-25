package com.chameleonvision.common.vision.processes;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameProvider;
import com.chameleonvision.common.vision.pipeline.CVPipeline;
import com.chameleonvision.common.vision.pipeline.CVPipelineResult;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** VisionRunner has a frame supplier, a pipeline supplier, and a result consumer */
@SuppressWarnings("rawtypes")
public class VisionRunner {

    private final Thread visionProcessThread;
    private final Supplier<Frame> frameSupplier;
    private final Supplier<CVPipeline> pipelineSupplier;
    private final Consumer<CVPipelineResult> pipelineResultConsumer;

    private long loopCount;

    /**
    * VisionRunner contains a <see cref="Thread">Thread</see> to run a pipeline, given a frame, and
    * will give the result to the consumer.
    *
    * @param frameSupplier The supplier of the latest frame.
    * @param pipelineSupplier The supplier of the current pipeline.
    * @param pipelineResultConsumer The consumer of the latest result.
    */
    public VisionRunner(
            FrameProvider frameSupplier,
            Supplier<CVPipeline> pipelineSupplier,
            Consumer<CVPipelineResult> pipelineResultConsumer) {
        this.frameSupplier = frameSupplier;
        this.pipelineSupplier = pipelineSupplier;
        this.pipelineResultConsumer = pipelineResultConsumer;

        this.visionProcessThread = new Thread(this::update);
        this.visionProcessThread.setName("VisionRunner - " + frameSupplier.getName());
    }

    public void startProcess() {
        visionProcessThread.start();
    }

    private boolean hasThrown;

    private void update() {
        while (!Thread.interrupted()) {
            loopCount++;
            var pipeline = pipelineSupplier.get();
            var frame = frameSupplier.get();

            try {
                var pipelineResult = pipeline.run(frame);
                pipelineResultConsumer.accept(pipelineResult);
            } catch (Exception ex) {
                if (hasThrown) {
                    System.err.println(
                            "Exception in thread \"" + visionProcessThread.getName() + "\", loop " + loopCount);
                    ex.printStackTrace();
                    hasThrown = true;
                }
            }
        }
    }
}

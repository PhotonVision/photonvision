package com.chameleonvision.common.vision.processes;

import com.chameleonvision.common.datatransfer.DataConsumer;
import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameConsumer;
import com.chameleonvision.common.vision.pipeline.CVPipelineResult;
import java.util.LinkedList;

/**
* This is the God Class
*
* <p>VisionModule has a pipeline manager, vision runner, and data providers. The data providers
* provide info on settings changes. VisionModuleManager holds a list of all current vision modules.
*/
public class VisionModule {

    private final PipelineManager pipelineManager;
    private final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final LinkedList<DataConsumer> dataConsumers = new LinkedList<>();
    private final LinkedList<FrameConsumer> frameConsumers = new LinkedList<>();

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource) {
        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        this.visionRunner =
                new VisionRunner(
                        this.visionSource.getFrameProvider(),
                        this.pipelineManager::getCurrentPipeline,
                        this::consumeResult);
    }

    public void start() {
        visionRunner.startProcess();
    }

    void consumeResult(CVPipelineResult result) {
        // TODO: put result in to Data
        var data = new Data();
        data.result = result;
        consumeData(data);

        var frame = result.outputFrame;
        consumeFrame(frame);
    }

    void consumeData(Data data) {
        for (var dataConsumer : dataConsumers) {
            dataConsumer.accept(data);
        }
    }

    public void addDataConsumer(DataConsumer dataConsumer) {
        dataConsumers.add(dataConsumer);
    }

    public void addFrameConsumer(FrameConsumer frameConsumer) {
        frameConsumers.add(frameConsumer);
    }

    void consumeFrame(Frame frame) {
        for (var frameConsumer : frameConsumers) {
            frameConsumer.accept(frame);
        }
    }
}

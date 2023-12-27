package org.photonvision.common.dataflow.statusLEDs;

import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class StatusLEDConsumer implements CVPipelineResultConsumer{

    private final int index;

    public StatusLEDConsumer(int index){
        this.index = index;
    }

    @Override
    public void accept(CVPipelineResult t) {
        HardwareManager.getInstance().setStatus()
    }
    
}

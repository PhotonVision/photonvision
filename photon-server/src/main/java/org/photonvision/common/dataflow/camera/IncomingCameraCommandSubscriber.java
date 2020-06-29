package org.photonvision.common.dataflow.camera;

import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.processes.VisionModuleManager;

public class IncomingCameraCommandSubscriber extends DataChangeSubscriber {
    private static final Logger logger = new Logger(IncomingCameraCommandSubscriber.class, LogGroup.Camera);

    private final VisionModuleManager vmm;

    public IncomingCameraCommandSubscriber(VisionModuleManager instance) {
        this.vmm = instance;
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent event) {
        logger.de_pest("Got event from [" + event.sourceType + "] and dest [" + event.destType
            + "] with property name [" + event.propertyName
            + "] and value [" + event.data + "]");
    }
}

package com.chameleonvision.common.vision.processes;

import com.chameleonvision.common.vision.frame.FrameProvider;

public interface VisionSource {
    FrameProvider getFrameProvider();

    VisionSourceSettables getSettables();
}

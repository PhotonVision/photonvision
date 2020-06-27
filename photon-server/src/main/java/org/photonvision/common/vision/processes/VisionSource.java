package org.photonvision.common.vision.processes;

import org.photonvision.common.vision.frame.FrameProvider;

public interface VisionSource {

    FrameProvider getFrameProvider();

    VisionSourceSettables getSettables();
}

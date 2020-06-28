package org.photonvision.vision.processes;

import org.photonvision.vision.frame.FrameProvider;

public interface VisionSource {

    FrameProvider getFrameProvider();

    VisionSourceSettables getSettables();
}

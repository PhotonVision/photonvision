package org.photonvision.vision.frame;

import java.util.function.Supplier;

public interface FrameProvider extends Supplier<Frame> {
    String getName();
}

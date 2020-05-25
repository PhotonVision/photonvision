package com.chameleonvision.common.vision.frame;

import java.util.function.Supplier;

public interface FrameProvider extends Supplier<Frame> {
    String getName();
}

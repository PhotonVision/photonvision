package com.chameleonvision.common.vision.frame.consumer;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameConsumer;

public class DummyFrameConsumer implements FrameConsumer {
    @Override
    public void accept(Frame frame) {
        frame.release(); // lol ez
    }
}

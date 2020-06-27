package org.photonvision.common.vision.frame.consumer;

import org.photonvision.common.vision.frame.Frame;
import org.photonvision.common.vision.frame.FrameConsumer;

public class DummyFrameConsumer implements FrameConsumer {
    @Override
    public void accept(Frame frame) {
        frame.release(); // lol ez
    }
}

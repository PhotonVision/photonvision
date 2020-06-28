package org.photonvision.vision.frame.consumer;

import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;

public class DummyFrameConsumer implements FrameConsumer {
    @Override
    public void accept(Frame frame) {
        frame.release(); // lol ez
    }
}

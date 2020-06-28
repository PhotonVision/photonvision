package org.photonvision.vision.frame.provider;

import org.apache.commons.lang3.NotImplementedException;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;

public class NetworkFrameProvider implements FrameProvider {
    private int count = 0;

    @Override
    public Frame get() {
        throw new NotImplementedException("");
    }

    @Override
    public String getName() {
        return "NetworkFrameProvider" + count++;
    }
}

package org.photonvision.common.vision.frame.provider;

import org.photonvision.common.vision.frame.Frame;
import org.photonvision.common.vision.frame.FrameProvider;
import org.apache.commons.lang3.NotImplementedException;

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

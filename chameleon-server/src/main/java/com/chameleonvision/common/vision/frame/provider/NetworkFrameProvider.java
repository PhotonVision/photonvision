package com.chameleonvision.common.vision.frame.provider;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameProvider;
import com.chameleonvision.common.vision.frame.FrameStaticProperties;
import org.apache.commons.lang3.NotImplementedException;

public class NetworkFrameProvider implements FrameProvider {
    @Override
    public Frame getFrame() {
        throw new NotImplementedException("");
    }

    @Override
    public FrameStaticProperties getFrameProperties() {
        throw new NotImplementedException("");
    }
}

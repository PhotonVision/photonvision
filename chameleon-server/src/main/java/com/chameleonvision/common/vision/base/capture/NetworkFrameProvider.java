package com.chameleonvision.common.vision.base.capture;

import com.chameleonvision.common.vision.base.frame.Frame;
import com.chameleonvision.common.vision.base.frame.FrameProvider;
import org.apache.commons.lang3.NotImplementedException;

public class NetworkFrameProvider implements FrameProvider {
    @Override
    public Frame getFrame() {
        throw new NotImplementedException("");
    }
}

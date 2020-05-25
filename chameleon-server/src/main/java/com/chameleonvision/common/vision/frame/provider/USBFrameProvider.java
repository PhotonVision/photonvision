package com.chameleonvision.common.vision.frame.provider;

import com.chameleonvision.common.vision.frame.Frame;
import com.chameleonvision.common.vision.frame.FrameProvider;
import org.apache.commons.lang3.NotImplementedException;

public class USBFrameProvider implements FrameProvider {
    private static int count = 0;

    @Override
    public Frame get() {
        throw new NotImplementedException("");
    }

    @Override
    public String getName() {
        return "USBFrameProvider" + count++;
    }
}

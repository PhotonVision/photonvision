package com.chameleonvision.common.vision.pipe;

public enum ImageFlipMode {
    NONE(Integer.MIN_VALUE),
    VERTICAL(1),
    HORIZONTAL(0),
    BOTH(-1);

    public final int value;

    ImageFlipMode(int value) {
        this.value = value;
    }
}

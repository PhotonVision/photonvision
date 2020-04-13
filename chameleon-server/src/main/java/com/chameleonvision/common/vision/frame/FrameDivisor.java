package com.chameleonvision.common.vision.frame;

public enum FrameDivisor {
    NONE(1),
    HALF(2),
    QUARTER(4),
    SIXTH(6);

    public final Integer value;

    FrameDivisor(int value) {
        this.value = value;
    }
}

package com.chameleonvision._2.vision.enums;

public enum StreamDivisor {
    NONE(1),
    HALF(2),
    QUARTER(4),
    SIXTH(6);

    public final Integer value;

    StreamDivisor(int value) {
        this.value = value;
    }
}

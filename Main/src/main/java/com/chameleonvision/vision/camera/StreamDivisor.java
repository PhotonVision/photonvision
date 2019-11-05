package com.chameleonvision.vision.camera;

public enum StreamDivisor {
    none(1),
    half(2),
    quarter(4),
    eighth(8);

    public final Integer value;

    StreamDivisor(int value) {
        this.value = value;
    }
}

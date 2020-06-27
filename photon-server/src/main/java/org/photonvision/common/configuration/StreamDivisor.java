package org.photonvision.common.configuration;

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

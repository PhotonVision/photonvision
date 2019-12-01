package com.chameleonvision.vision.enums;

import org.opencv.core.Core;

public enum ImageRotationMode {
    DEG_0(-1),
    DEG_90(Core.ROTATE_90_CLOCKWISE),
    DEG_180(Core.ROTATE_180),
    DEG_270(Core.ROTATE_90_COUNTERCLOCKWISE);

    public final int value;

    ImageRotationMode(int value) {
        this.value = value;
    }
}

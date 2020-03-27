package com.chameleonvision._2.vision.enums;

public enum ImageRotationMode {
    DEG_0(-1),
    DEG_90(0),
    DEG_180(1),
    DEG_270(2);

    public final int value;

    ImageRotationMode(int value) {
        this.value = value;
    }

    public boolean isRotated(){return this.value==DEG_90.value||this.value==DEG_270.value;}
}

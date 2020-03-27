package com.chameleonvision.common.vision.base.pipeline.pipe.params;

public class RotateImageParams {

    public static RotateImageParams DEFAULT = new RotateImageParams(ImageRotation.DEG_0);

    public ImageRotation rotation;

    public RotateImageParams() {
        rotation = DEFAULT.rotation;
    }

    public RotateImageParams(ImageRotation rotation) {
        this.rotation = rotation;
    }

    public enum ImageRotation {
        DEG_0(-1),
        DEG_90(0),
        DEG_180(1),
        DEG_270(2);

        public final int value;

        ImageRotation(int value) {
            this.value = value;
        }

        public boolean isRotated() {
            return this.value==DEG_90.value || this.value==DEG_270.value;
        }
    }
}

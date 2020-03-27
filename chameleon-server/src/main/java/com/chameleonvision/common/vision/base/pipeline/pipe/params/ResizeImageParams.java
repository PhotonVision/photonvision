package com.chameleonvision.common.vision.base.pipeline.pipe.params;

import org.opencv.core.Size;

public class ResizeImageParams {

    public static ResizeImageParams DEFAULT = new ResizeImageParams(320, 240);

    private Size size;
    public int width;
    public int height;

    public ResizeImageParams() {
        this(DEFAULT.width, DEFAULT.height);
    }

    public ResizeImageParams(int width, int height) {
        this.width = width;
        this.height = height;
        size = new Size(new double[]{width, height});
    }

    public Size getSize() {
        return size;
    }
}

package com.chameleonvision.vision.camera;

import edu.wpi.cscore.VideoMode;

public class CamVideoMode {
    public final int fps;
    public final int width;
    public final int height;
    public final String pixel_format;

    public CamVideoMode(VideoMode videoMode) {
        fps = videoMode.fps;
        width = videoMode.width;
        height = videoMode.height;
        pixel_format = videoMode.pixelFormat.name();
    }

    public VideoMode.PixelFormat getActualPixelFormat() {
        return VideoMode.PixelFormat.valueOf(pixel_format);
    }

    public boolean isEqualToVideoMode(VideoMode videoMode) {
        return videoMode.fps == fps && videoMode.width == width && videoMode.height == height && videoMode.pixelFormat == getActualPixelFormat();
    }
}

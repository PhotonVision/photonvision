package com.chameleonvision.classabstraction.camera;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;

public class USBCamera {
    private final UsbCamera baseCamera;
    public final CameraProperties properties;

    public USBCamera(UsbCamera camera) {
        baseCamera = camera;
        VideoMode vidMode = new VideoMode(VideoMode.PixelFormat.kYUYV, 640, 480, 60);
        properties = new CameraProperties(baseCamera, 75);
    }
}

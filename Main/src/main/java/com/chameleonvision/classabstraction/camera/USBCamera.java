package com.chameleonvision.classabstraction.camera;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoException;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class USBCamera implements CameraProcess {
    private final UsbCamera baseCamera;
    public final CameraProperties properties;
    private final CvSink cvSink;
    private Mat imageBuffer = new Mat();

    public USBCamera(UsbCamera camera, double fov) {
        baseCamera = camera;
        cvSink = CameraServer.getInstance().getVideo(baseCamera);
        VideoMode vidMode = new VideoMode(VideoMode.PixelFormat.kYUYV, 640, 480, 60);
        properties = new CameraProperties(baseCamera, fov);
    }

    @Override
    public Pair<Mat, Long> getFrame(Mat frame) {
        var timestamp = System.nanoTime();
        cvSink.grabFrame(imageBuffer);
        imageBuffer.copyTo(frame);
        return Pair.of(frame, timestamp - System.nanoTime());
    }

    @Override
    public void setExposure(int exposure) {
        try {
            baseCamera.setExposureManual(exposure);
        } catch (VideoException e) {
            System.err.println("USBCamera Does not support exposure change");
        }
    }

    @Override
    public void setBrightness(int brightness) {
        try {
            baseCamera.setBrightness(brightness);
        } catch (VideoException e) {
            System.err.println("USBCamera Does not support brightness change");
        }
    }
}

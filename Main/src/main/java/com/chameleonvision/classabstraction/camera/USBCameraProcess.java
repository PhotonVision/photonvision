package com.chameleonvision.classabstraction.camera;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoException;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class USBCameraProcess implements CameraProcess {
    private final UsbCamera baseCamera;
    private final CvSink cvSink;
    private Mat imageBuffer = new Mat();
    public final CameraProperties properties;

    public USBCameraProcess(UsbCamera camera, CameraConfig config) {
        baseCamera = camera;
        cvSink = CameraServer.getInstance().getVideo(baseCamera);
        VideoMode vidMode = new VideoMode(VideoMode.PixelFormat.kYUYV, 640, 480, 60);
        properties = new CameraProperties(baseCamera, config.FOV);
    }

    @Override
    public CameraProperties getProperties() {
        return properties;
    }

    @Override
    public Pair<Mat, Long> getFrame(Mat frame) {
        Long deltaTime;
        synchronized (cvSink) {
            deltaTime = cvSink.grabFrame(imageBuffer) * 1000L;
            imageBuffer.copyTo(frame);
        }
        return Pair.of(frame, deltaTime);
    }

    @Override
    public void setExposure(int exposure) {
        try {
            baseCamera.setExposureManual(exposure);
        } catch (VideoException e) {
            System.err.println("Current camera does not support exposure change");
        }
    }

    @Override
    public void setBrightness(int brightness) {
        try {
            baseCamera.setBrightness(brightness);
        } catch (VideoException e) {
            System.err.println("Current camera does not support brightness change");
        }
    }
}

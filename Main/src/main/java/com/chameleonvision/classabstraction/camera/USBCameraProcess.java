package com.chameleonvision.classabstraction.camera;

import com.chameleonvision.classabstraction.config.CameraConfig;
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
    private CameraProperties properties;

    public USBCameraProcess(UsbCamera camera, CameraConfig config) {
        baseCamera = camera;
        cvSink = CameraServer.getInstance().getVideo(baseCamera);
        properties = new CameraProperties(baseCamera, config.FOV);

        setVideoMode(properties.videoModes.get(0));
    }

    @Override
    public CameraProperties getProperties() {
        return properties;
    }

    @Override
    public Pair<Mat, Long> getFrame() {
        Long deltaTime;
        synchronized (cvSink) {
            deltaTime = cvSink.grabFrame(imageBuffer) * 1000L;
        }
        return Pair.of(imageBuffer, deltaTime);
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

    @Override
    public void setVideoMode(VideoMode mode) {
        try {
            baseCamera.setVideoMode(mode);
            properties.updateVideoMode(mode);
        } catch (VideoException e) {
            System.err.println("Current camera does not support resolution change");
        }
    }
}

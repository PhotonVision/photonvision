package com.chameleonvision.vision.camera;

import com.chameleonvision.config.CameraJsonConfig;
import com.chameleonvision.vision.image.CaptureProperties;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoException;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class USBCameraCapture implements CameraCapture {
    private final UsbCamera baseCamera;
    private final CvSink cvSink;
    private Mat imageBuffer = new Mat();
    private USBCaptureProperties properties;

    public USBCameraCapture(CameraJsonConfig config) {
        baseCamera = new UsbCamera(config.name, config.path);
        cvSink = CameraServer.getInstance().getVideo(baseCamera);
        properties = new USBCaptureProperties(baseCamera, config);

        int videoMode = properties.videoModes.size() - 1 <= config.videomode ? config.videomode : 0;
        setVideoMode(videoMode);
    }


    @Override
    public USBCaptureProperties getProperties() {
        return properties;
    }

    @Override
    public VideoMode getCurrentVideoMode() {
        return baseCamera.getVideoMode();
    }

    @Override
    public Pair<Mat, Long> getFrame() {
        Long deltaTime;
        // TODO: Why multiply by 1000 here?
        deltaTime = cvSink.grabFrame(imageBuffer) * 1000L;
        return Pair.of(imageBuffer, deltaTime);
    }

    @Override
    public void setExposure(int exposure) {
        try {
            baseCamera.setExposureManual(exposure);
        } catch (VideoException e) {
            System.err.println("Failed to change camera exposure!");
        }
    }

    @Override
    public void setBrightness(int brightness) {
        try {
            baseCamera.setBrightness(brightness);
        } catch (VideoException e) {
            System.err.println("Failed to change camera brightness!");
        }
    }

    @Override
    public void setVideoMode(VideoMode mode) {
        try {
            baseCamera.setVideoMode(mode);
            properties.updateVideoMode(mode);
        } catch (VideoException e) {
            System.err.println("Failed to change camera video mode!");
        }
    }

    public void setVideoMode(int index){
        VideoMode mode = properties.getVideoModes().get(index);
        setVideoMode(mode);
    }

    @Override
    public void setGain(int gain) {
        if (properties.isPS3Eye) {
            try {
                baseCamera.getProperty("gain_automatic").set(0);
                baseCamera.getProperty("gain").set(gain);
            } catch (Exception e) {
                System.err.println("Failed to change camera gain!");
            }
        }
    }
}

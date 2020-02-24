package com.chameleonvision.vision.camera;

import com.chameleonvision.config.CameraCalibrationConfig;
import com.chameleonvision.config.FullCameraConfiguration;
import com.chameleonvision.util.Helpers;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoException;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class USBCameraCapture implements CameraCapture {
    private final UsbCamera baseCamera;
    private final CvSink cvSink;
    private List<CameraCalibrationConfig> calibrationList;
    private Mat imageBuffer = new Mat();
    private USBCaptureProperties properties;

    public USBCameraCapture(FullCameraConfiguration fullCameraConfiguration) {
        var config = fullCameraConfiguration.cameraConfig;
        this.calibrationList = new ArrayList<>(); //fullCameraConfiguration.calibration;
        calibrationList.addAll(fullCameraConfiguration.calibration);
        baseCamera = new UsbCamera(config.name, config.path);
        cvSink = CameraServer.getInstance().getVideo(baseCamera);
        properties = new USBCaptureProperties(baseCamera, config);

        var videoModes = properties.getVideoModes();
        if(videoModes.size() < 1) {
            throw new VideoException("0 video modes are valid! Full list provided by camera: \n\n"
            + Arrays.stream(baseCamera.enumerateVideoModes()).map(Helpers::VideoModeToHashMap).toString()  );
        }

        int videoMode = properties.videoModes.size() - 1 <= config.videomode ? config.videomode : 0;
        setVideoMode(videoMode);
    }

    public CameraCalibrationConfig getCalibration(Size size) {
        for(var calibration: calibrationList) {
            if(calibration.resolution.equals(size)) return calibration;
        }
        return null;
    }

    public CameraCalibrationConfig getCalibration(VideoMode mode) {
        return getCalibration(new Size(mode.width, mode.height));
    }

    public void addCalibrationData(CameraCalibrationConfig newConfig) {
        calibrationList.removeIf(c -> newConfig.resolution.height == c.resolution.height && newConfig.resolution.width == c.resolution.width);
        calibrationList.add(newConfig);
    }

    public List<CameraCalibrationConfig> getConfig() {
        return calibrationList;
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
        Mat tempMat = new Mat();
        deltaTime = cvSink.grabFrame(tempMat) * 1000L;
//        tempMat = Imgcodecs.imread("C:\\Users\\imadu\\Documents\\GitHub\\chameleon-vision\\chameleon-server\\testimages\\2020\\image.png");
        tempMat.copyTo(imageBuffer);
        tempMat.release();
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

    @Override
    public CameraCalibrationConfig getCurrentCalibrationData() {
        return getCalibration(getCurrentVideoMode());
    }

    @Override
    public List<CameraCalibrationConfig> getAllCalibrationData() {
        return calibrationList;
    }
}

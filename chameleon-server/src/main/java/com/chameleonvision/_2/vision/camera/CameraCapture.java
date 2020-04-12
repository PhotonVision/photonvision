package com.chameleonvision._2.vision.camera;

import com.chameleonvision._2.config.CameraCalibrationConfig;
import com.chameleonvision._2.vision.image.CaptureProperties;
import com.chameleonvision._2.vision.image.ImageCapture;
import edu.wpi.cscore.VideoMode;
import java.util.List;

public interface CameraCapture extends ImageCapture {
    CaptureProperties getProperties();

    VideoMode getCurrentVideoMode();

    /**
    * Set the exposure of the camera
    *
    * @param exposure the new exposure to set the camera to
    */
    void setExposure(int exposure);

    /**
    * Set the brightness of the camera
    *
    * @param brightness the new brightness to set the camera to
    */
    void setBrightness(int brightness);

    /**
    * Set the video mode (fps and resolution) of the camera
    *
    * @param mode the desired mode
    */
    void setVideoMode(VideoMode mode);

    /**
    * Set the video mode (fps and resolution) of the camera
    *
    * @param index the index of the desired mode
    */
    void setVideoMode(int index);

    /**
    * Set the gain of the camera NOTE - Not all cameras support this.
    *
    * @param gain the new gain to set the camera to
    */
    void setGain(int gain);

    CameraCalibrationConfig getCurrentCalibrationData();

    List<CameraCalibrationConfig> getAllCalibrationData();
}

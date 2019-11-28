package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.image.ImageCapture;
import edu.wpi.cscore.VideoMode;

public interface CameraCapture extends ImageCapture {
    USBCameraProperties getProperties();

    public VideoMode getCurrentVideoMode();

    /**
     * Set the exposure of the camera
     * @param exposure the new exposure to set the camera to
     */
    void setExposure(int exposure);

    /**
     * Set the brightness of the camera
     * @param brightness the new brightness to set the camera to
     */
    void setBrightness(int brightness);

    /**
     * Set the video mode (fps and resolution) of the camera
     * @param mode the wanted mode
     */
    void setVideoMode(VideoMode mode);

    /**
     * Set the gain of the camera
     * NOTE - Not all cameras support this.
     * @param gain the new gain to set the camera to
     */
    void setGain(int gain);
}

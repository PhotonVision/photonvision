package com.chameleonvision.classabstraction.camera;

import edu.wpi.cscore.VideoMode;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public interface CameraProcess {
    CameraProperties getProperties();

    /**
     * Get the next camera frame
     * @return a Pair of the captured image and how long it took to grab the frame (in uS)
     */
    Pair<Mat, Long> getFrame();

    /**
     * Set the exposure of the camera
     * @param exposure the new exposure to set the camera to
     */
    void setExposure(int exposure);

    /**
     * Set the exposure of the camera
     * @param brightness the new brightness to set the camera to
     */
    void setBrightness(int brightness);

    /**
     * Set the video mode (fps and resolution) of the camera
     * @param mode the wanted mode
     */
    void setVideoMode(VideoMode mode);
}

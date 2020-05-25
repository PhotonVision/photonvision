package com.chameleonvision.common.vision.processes;

import edu.wpi.cscore.VideoMode;
import java.util.Dictionary;

public interface VisionSourceSettables {
    int getExposure();

    void setExposure(int exposure);

    int getBrightness();

    void setBrightness(int brightness);

    int getGain();

    void setGain(int gain);

    VideoMode getCurrentVideoMode();

    void setCurrentVideoMode(VideoMode videoMode);

    Dictionary<Integer, VideoMode> getAllVideoModes();
}

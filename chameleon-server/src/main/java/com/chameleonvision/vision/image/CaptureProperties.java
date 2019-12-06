package com.chameleonvision.vision.image;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import edu.wpi.cscore.VideoMode;
import org.opencv.core.Mat;

public class CaptureProperties {

    protected CaptureStaticProperties staticProperties;

    protected CaptureProperties() {
    }

    public CaptureProperties(VideoMode videoMode, double fov) {
        staticProperties = new CaptureStaticProperties(videoMode, fov);
    }
    public void setStaticProperties(CaptureStaticProperties staticProperties) {this.staticProperties = staticProperties;}
    public CaptureStaticProperties getStaticProperties() {
        return staticProperties;
    }
}

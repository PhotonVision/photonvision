package com.chameleonvision.vision.image;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import edu.wpi.cscore.VideoMode;
import org.opencv.core.Mat;

public class CaptureProperties {

    protected CaptureStaticProperties staticProperties;

    protected CaptureProperties() {
    }

    public CaptureProperties(Mat staticImage, double fov) {
        staticProperties = new CaptureStaticProperties(new VideoMode(0, staticImage.cols(), staticImage.rows(), 99999), staticImage.cols(), staticImage.rows(), fov);
    }

    public CaptureStaticProperties getStaticProperties() {
        return staticProperties;
    }
}

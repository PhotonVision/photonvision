package com.chameleonvision.vision.image;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import org.opencv.core.Mat;

public class CaptureProperties {

    protected CaptureStaticProperties staticProperties;

    protected CaptureProperties() {
    }

    public CaptureProperties(Mat staticImage, double fov) {
        staticProperties = new CaptureStaticProperties(staticImage.cols(), staticImage.rows(), fov);
    }

    public CaptureStaticProperties getStaticProperties() {
        return staticProperties;
    }
}

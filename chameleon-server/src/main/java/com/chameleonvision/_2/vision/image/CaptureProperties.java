package com.chameleonvision._2.vision.image;

import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;

public class CaptureProperties {

    protected CaptureStaticProperties staticProperties;
    private Rotation2d tilt = new Rotation2d();

    protected CaptureProperties() {
    }

    public CaptureProperties(VideoMode videoMode, double fov) {
        staticProperties = new CaptureStaticProperties(videoMode, fov);
    }
    public void setStaticProperties(CaptureStaticProperties staticProperties) {this.staticProperties = staticProperties;}
    public CaptureStaticProperties getStaticProperties() {
        return staticProperties;
    }

    public Rotation2d getTilt() {
        return tilt;
    }

    public void setTilt(Rotation2d tilt) {
        this.tilt = tilt;
    }
}

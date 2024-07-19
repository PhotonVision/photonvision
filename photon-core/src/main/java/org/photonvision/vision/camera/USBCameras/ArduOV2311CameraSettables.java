package org.photonvision.vision.camera.USBCameras;

import edu.wpi.first.cscore.UsbCamera;


import org.photonvision.common.configuration.CameraConfiguration;

public class ArduOV2311CameraSettables extends GenericUSBCameraSettables {

    public ArduOV2311CameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        super.setUpExposureProperties();

        // Property limits are incorrect
        this.minExposure = 1;
        this.maxExposure = 75;
    }

}

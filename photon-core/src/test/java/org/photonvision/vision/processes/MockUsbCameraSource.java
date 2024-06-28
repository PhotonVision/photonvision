package org.photonvision.vision.processes;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.provider.FileFrameProvider;

public class MockUsbCameraSource extends USBCameraSource{

    /**
     * Used for unit tests to better simulate a usb camera without a camera being present.
     */
    public MockUsbCameraSource(CameraConfiguration config, int pid, int vid) {
        super(config);

        getCameraConfiguration().cameraQuirks = QuirkyCamera.getQuirkyCamera(pid, vid, config.baseName);

        /**
         * File used as frame provider
         */
        usbFrameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(
                                TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        usbCameraSettables = new MockUsbCameraSettables(config);
        
    }

    private class MockUsbCameraSettables extends USBCameraSettables{

        public MockUsbCameraSettables(CameraConfiguration config){
            super(config);
        }

        /**
         * Hardware-specific implementation - do nothing in test
         */
        @Override
        public void setExposureRaw(double exposureRaw){}

        /**
         * Hardware-specific implementation - do nothing in test
         */
        @Override
        public void setAutoExposure(boolean cameraAutoExposure) {};

    }
    
}

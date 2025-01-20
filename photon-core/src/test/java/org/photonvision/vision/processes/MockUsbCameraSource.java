/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.processes;

import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoMode;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameras.GenericUSBCameraSettables;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.frame.provider.FileFrameProvider;

public class MockUsbCameraSource extends USBCameraSource {
    /** Used for unit tests to better simulate a usb camera without a camera being present. */
    public MockUsbCameraSource(CameraConfiguration config, int pid, int vid) {
        super(config);

        getCameraConfiguration().cameraQuirks =
                QuirkyCamera.getQuirkyCamera(pid, vid, config.matchedCameraInfo.name());

        /** File used as frame provider */
        usbFrameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        this.settables = createSettables(config, null);
    }

    @Override
    public GenericUSBCameraSettables createSettables(CameraConfiguration config, UsbCamera camera) {
        return new MockUsbCameraSettables(config, null);
    }

    private class MockUsbCameraSettables extends GenericUSBCameraSettables {
        public MockUsbCameraSettables(CameraConfiguration config, UsbCamera camera) {
            super(config, camera);
        }

        /** Hardware-specific implementation - do nothing in test */
        @Override
        public void setExposureRaw(double exposureRaw) {}

        /** Hardware-specific implementation - do nothing in test */
        @Override
        public void setAutoExposure(boolean cameraAutoExposure) {}

        /** Hardware-specific implementation - do nothing in test */
        @Override
        public void setBrightness(int brightness) {}

        @Override
        public void setGain(int gain) {}

        @Override
        public void setVideoModeInternal(VideoMode videoMode) {}

        @Override
        public void setUpExposureProperties() {}

        @Override
        protected void setUpWhiteBalanceProperties() {}

        @Override
        public void setWhiteBalanceTemp(double tempNumber) {}

        @Override
        public void setAutoWhiteBalance(boolean autoWB) {}

        @Override
        public double getMinWhiteBalanceTemp() {
            return 1;
        }

        @Override
        public double getMaxWhiteBalanceTemp() {
            return 2;
        }
    }
}

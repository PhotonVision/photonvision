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

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.provider.FileFrameProvider;

public class MockUsbCameraSource extends USBCameraSource {
    /** Used for unit tests to better simulate a usb camera without a camera being present. */
    public MockUsbCameraSource(CameraConfiguration config, int pid, int vid) {
        super(config);

        getCameraConfiguration().cameraQuirks = QuirkyCamera.getQuirkyCamera(pid, vid, config.baseName);

        /** File used as frame provider */
        usbFrameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        usbCameraSettables = new MockUsbCameraSettables(config);
    }

    private class MockUsbCameraSettables extends USBCameraSettables {
        public MockUsbCameraSettables(CameraConfiguration config) {
            super(config);
        }

        /** Hardware-specific implementation - do nothing in test */
        @Override
        public void setExposureRaw(double exposureRaw) {}

        /** Hardware-specific implementation - do nothing in test */
        @Override
        public void setAutoExposure(boolean cameraAutoExposure) {}
        ;
    }
}

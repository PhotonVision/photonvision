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

package org.photonvision.vision.camera.USBCameras;

import org.photonvision.common.configuration.CameraConfiguration;
import org.wpilib.vision.camera.UsbCamera;

public class ThriftyOV9281CameraSettables extends GenericUSBCameraSettables {
    // V4L2 exposure_absolute units (100 μs each). Range 1-2400 is 0.1 ms-240 ms.
    private static final double MIN_EXPOSURE_RAW = 1;
    private static final double MAX_EXPOSURE_RAW = 2400;
    // 80 = 8 ms;
    public static final double DEFAULT_EXPOSURE_RAW = 80;

    public ThriftyOV9281CameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        super.setUpExposureProperties();

        this.minExposure = MIN_EXPOSURE_RAW;
        this.maxExposure = MAX_EXPOSURE_RAW;
    }

    @Override
    public void setAllCamDefaults() {
        // Disable continuous autofocus BEFORE super tries to set focus_absolute
        softSet("focus_automatic_continuous", 0);
        super.setAllCamDefaults();
        logger.info("Setting All Cam Defaults :: ThriftyOV9281");
    }
}

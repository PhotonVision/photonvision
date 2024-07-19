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

import edu.wpi.first.cscore.UsbCamera;
import org.photonvision.common.configuration.CameraConfiguration;

public class ArduOV9281CameraSettables extends GenericUSBCameraSettables {
    public ArduOV9281CameraSettables(CameraConfiguration configuration, UsbCamera camera) {
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

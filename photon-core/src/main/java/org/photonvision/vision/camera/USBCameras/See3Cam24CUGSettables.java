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

/*
 * This class holds the camera quirks for the See3Cam 24UGS.
 */
public class See3Cam24CUGSettables extends GenericUSBCameraSettables {
    public See3Cam24CUGSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        super.setUpExposureProperties();

        // Property limits are incorrect
        this.minExposure = 0;
        this.maxExposure = 600;
    }
}

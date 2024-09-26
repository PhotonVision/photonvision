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

public class PsEyeCameraSettables extends GenericUSBCameraSettables {
    public PsEyeCameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);

        // PS Eye uses inverted 1=Disabled, 0=Enabled for auto exposure
        if (!cameraAutoExposure) {
            autoExposureProp.set(1);

            // Most cameras leave exposure time absolute at the last value
            // from their auto exposure algorithm.
            // Set it back to the exposure slider value
            setExposureRaw(this.lastExposureRaw);

        } else {
            autoExposureProp.set(0);
        }
    }

    @Override
    public void setAllCamDefaults() {
        // Common settings for all cameras to attempt to get their image
        // as close as possible to what we want for image processing
        softSet("raw_hue", 0);
        softSet("raw_contrast", 32);
        softSet("raw_saturation", 100);
        softSet("raw_hue", -10);
        softSet("raw_sharpness", 0);
        softSet("white_balance_automatic", 0);
        softSet("gain_automatic", 0);
    }
}

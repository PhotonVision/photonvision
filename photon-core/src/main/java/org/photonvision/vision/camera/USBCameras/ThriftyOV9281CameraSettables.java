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
import org.wpilib.util.PixelFormat;
import org.wpilib.vision.camera.UsbCamera;

public class ThriftyOV9281CameraSettables extends GenericUSBCameraSettables {

    public ThriftyOV9281CameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        super.setUpExposureProperties();

        // Fix the exposure lower and upper limits.
        // The minimum usable exposure is above the UI default of 20, so
        // the user is expected to increase exposure until the camera can
        // pick up an image correctly.
        this.minExposure = 1;
        this.maxExposure = 2400;
    }

    @Override
    public void onCameraConnected() {
        super.onCameraConnected();

        // Filter out YUYV modes. The Sunplus SPCA2688 ISP's MJPEG encoder
        // breaks permanently if a YUYV↔MJPEG format switch occurs.
        // Only MJPEG modes (120fps) are usable; YUYV modes are 5-30fps anyway.
        int originalSize = videoModes.size();
        videoModes.removeIf(m -> m.pixelFormat != PixelFormat.MJPEG);
        if (videoModes.size() < originalSize) {
            logger.info("Filtered to " + videoModes.size() + " MJPEG-only modes (YUYV removed)");
        }
    }

    @Override
    public void setAllCamDefaults() {
        // Disable continuous autofocus BEFORE super tries to set focus_absolute
        softSet("focus_automatic_continuous", 0);
        super.setAllCamDefaults();
        logger.info("Setting All Cam Defaults :: ThriftyOV9281");
        softSet("focus_absolute", 0);
    }

    @Override
    public void setAutoExposureImpl(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure :: ThriftyOV9281 :: " + cameraAutoExposure);
        if (autoExposureProp != null) {
            autoExposureProp.set(
                    cameraAutoExposure ? PROP_AUTO_EXPOSURE_ENABLED : PROP_AUTO_EXPOSURE_DISABLED);
        }
        if (!cameraAutoExposure) {
            setExposureRaw(this.lastExposureRaw);
        }
    }
}

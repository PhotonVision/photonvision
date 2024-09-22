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
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.math.MathUtil;
import org.photonvision.common.configuration.CameraConfiguration;

public class LifeCam3kWindowsCameraSettables extends GenericUSBCameraSettables {
    public LifeCam3kWindowsCameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        autoExposureProp = null; // Not Used
        exposureAbsProp = null; // Not Used

        // We'll fallback on cscore's implementation for windows lifecam
        this.minExposure = 0;
        this.maxExposure = 100;
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        if (exposureRaw >= 0.0) {
            try {
                int propVal = (int) MathUtil.clamp(exposureRaw, minExposure, maxExposure);

                // exposureAbsProp.set(propVal);
                camera.setExposureManual(propVal);

                this.lastExposureRaw = exposureRaw;

                // Lifecam requires setting brightness again after exposure
                // And it requires setting it twice, ensuring the value is different
                // This camera is very bork.
                if (lastBrightness >= 0) {
                    setBrightness(lastBrightness - 1);
                }

            } catch (VideoException e) {
                logger.error("Failed to set camera exposure!", e);
            }
        }
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);

        if (!cameraAutoExposure) {
            // Most cameras leave exposure time absolute at the last value from their AE
            // algorithm.
            // Set it back to the exposure slider value
            camera.setExposureManual((int) this.lastExposureRaw);
        } else {
            camera.setExposureAuto();
        }
    }

    @Override
    public void setAllCamDefaults() {
        // Common settings for all cameras to attempt to get their image
        // as close as possible to what we want for image processing
        softSet("raw_Contrast", 5);
        softSet("raw_Saturation", 85);
        softSet("raw_Sharpness", 25);
        softSet("WhiteBalance", 4000);
    }
}

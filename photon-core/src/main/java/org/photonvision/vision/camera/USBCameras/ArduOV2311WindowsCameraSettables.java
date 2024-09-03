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

/*
 * This class holds the windows specific camera quirks for the Arducam ov2311. A windows version is needed because windows doesn't expose the auto exposure properties of the arducam.
 */
public class ArduOV2311WindowsCameraSettables extends GenericUSBCameraSettables {
    public ArduOV2311WindowsCameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        var expProp =
                findProperty(
                        "raw_exposure_absolute",
                        "raw_exposure_time_absolute",
                        "exposure",
                        "raw_Exposure",
                        "Exposure");

        exposureAbsProp = expProp.get();
        autoExposureProp = null;
        this.minExposure = 1;
        this.maxExposure = 140;
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        if (exposureRaw >= 0.0) {
            try {
                int propVal = (int) MathUtil.clamp(exposureRaw, minExposure, maxExposure);
                camera.setExposureManual(propVal);
                this.lastExposureRaw = exposureRaw;
            } catch (VideoException e) {
                logger.error("Failed to set camera exposure!", e);
            }
        }
    }

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
}

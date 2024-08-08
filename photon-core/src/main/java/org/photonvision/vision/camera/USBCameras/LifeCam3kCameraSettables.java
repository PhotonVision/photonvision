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
import org.photonvision.common.util.math.MathUtils;

public class LifeCam3kCameraSettables extends GenericUSBCameraSettables {
    // Lifecam only allows specific exposures. Pulled this list from
    // https://github.com/wpilibsuite/allwpilib/blob/main/cscore/src/main/native/linux/UsbCameraImpl.cpp#L129
    private static int[] allowableExposures = {5, 10, 20, 39, 78, 156, 312, 625};

    public LifeCam3kCameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {
        autoExposureProp = findProperty("exposure_auto", "auto_exposure").get();
        exposureAbsProp = findProperty("raw_exposure_time_absolute", "raw_exposure_absolute").get();

        this.minExposure = exposureAbsProp.getMin();
        this.maxExposure = exposureAbsProp.getMax();
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        if (exposureRaw >= 0.0) {
            try {
                int propVal = (int) MathUtil.clamp(exposureRaw, minExposure, maxExposure);

                propVal = MathUtils.quantize(propVal, allowableExposures);

                logger.debug(
                        "Setting property "
                                + autoExposureProp.getName()
                                + " to "
                                + propVal
                                + " (user requested "
                                + exposureRaw
                                + " )");

                exposureAbsProp.set(propVal);

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
    public void setAllCamDefaults() {
        // Common settings for all cameras to attempt to get their image
        // as close as possible to what we want for image processing
        softSet("raw_contrast", 5);
        softSet("raw_saturation", 85);
        softSet("raw_sharpness", 25);
        softSet("white_balance_automatic", 0);
        softSet("white_balance_temperature", 4000);
    }
}

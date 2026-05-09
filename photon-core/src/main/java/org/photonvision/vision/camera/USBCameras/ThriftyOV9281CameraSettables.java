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

import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.wpilib.util.PixelFormat;
import org.wpilib.vision.camera.UsbCamera;
import org.wpilib.vision.camera.VideoMode;

public class ThriftyOV9281CameraSettables extends GenericUSBCameraSettables {

    // The Sunplus SPCA2688 ISP only accepts these discrete exposure values.
    // Any other value produces a completely black frame in MJPEG mode.
    // Values are in V4L2 exposure_time_absolute units (100µs each).
    private static final int[] DISCRETE_EXPOSURES = {
        5, 10, 20, 39, 78, 156, 312, 625, 1250, 2500, 5000
    };

    public ThriftyOV9281CameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    public void onCameraConnected() {
        super.onCameraConnected();

        // Filter out YUYV modes. The Sunplus SPCA2688 ISP's MJPEG encoder
        // breaks permanently if a YUYV↔MJPEG format switch occurs.
        // Only MJPEG modes (120fps) are usable; YUYV modes are 5-30fps anyway.
        HashMap<Integer, VideoMode> mjpegOnly = new HashMap<>();
        int newIdx = 0;
        for (var entry : videoModes.entrySet()) {
            if (entry.getValue().pixelFormat == PixelFormat.kMJPEG) {
                mjpegOnly.put(newIdx++, entry.getValue());
            }
        }
        if (!mjpegOnly.isEmpty()) {
            videoModes = mjpegOnly;
            logger.info("Filtered to " + mjpegOnly.size() + " MJPEG-only modes (YUYV removed)");
        }
    }

    @Override
    protected void setUpExposureProperties() {
        autoExposureProp = findProperty("exposure_auto", "auto_exposure").orElse(null);
        exposureAbsProp =
                findProperty("raw_exposure_time_absolute", "raw_exposure_absolute").orElse(null);

        // Expose the discrete range bounds to the PV UI slider.
        this.minExposure = DISCRETE_EXPOSURES[0];
        this.maxExposure = DISCRETE_EXPOSURES[DISCRETE_EXPOSURES.length - 1];
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

    @Override
    public void setExposureRaw(double exposureRaw) {
        if (exposureRaw >= 0.0 && exposureAbsProp != null) {
            int snapped = snapToDiscreteExposure((int) Math.round(exposureRaw));
            logger.debug(
                    "ThriftyOV9281: Setting "
                            + exposureAbsProp.getName()
                            + " to "
                            + snapped
                            + " (requested "
                            + exposureRaw
                            + ")");
            exposureAbsProp.set(snapped);
            this.lastExposureRaw = snapped;
        }
    }

    /**
     * Snap a requested exposure value to the nearest valid discrete value. The Sunplus SPCA2688 ISP
     * only responds to specific exposure values; any other value produces a completely black frame.
     */
    private static int snapToDiscreteExposure(int requested) {
        int closest = DISCRETE_EXPOSURES[0];
        int minDist = Math.abs(requested - closest);
        for (int discrete : DISCRETE_EXPOSURES) {
            int dist = Math.abs(requested - discrete);
            if (dist < minDist) {
                minDist = dist;
                closest = discrete;
            }
        }
        return closest;
    }
}

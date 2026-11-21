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

package org.photonvision.vision.camera;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.DuplicateFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

/**
 * A VisionSource that wraps another VisionSource to create a duplicate camera. This allows multiple
 * pipelines to process frames from the same physical camera.
 *
 * <p>The duplicate camera creates its own frame provider using the source camera's UsbCamera
 * object. This ensures each duplicate has its own CvSink to avoid cscore thread-safety issues.
 * Input settings (exposure, gain, white balance, etc.) are read-only and controlled by the source
 * camera.
 */
public class DuplicateVisionSource extends VisionSource {
    private final VisionSource sourceVisionSource;
    private final DuplicateFrameProvider frameProvider;
    private final DuplicateSettables settables;
    private final Logger logger;

    /**
     * Create a duplicate vision source from an existing source camera.
     *
     * @param config Configuration for this duplicate (has its own pipelines, nickname, etc.)
     * @param sourceVisionSource The source camera to duplicate from
     */
    public DuplicateVisionSource(CameraConfiguration config, VisionSource sourceVisionSource) {
        super(config);

        this.sourceVisionSource = sourceVisionSource;
        this.logger = new Logger(DuplicateVisionSource.class, config.nickname, LogGroup.Camera);

        // Create read-only settables that delegate to source
        this.settables = new DuplicateSettables(config, sourceVisionSource.getSettables());

        // Create our own frame provider using the source's UsbCamera to avoid thread-safety issues
        if (sourceVisionSource instanceof USBCameraSource usbSource) {
            this.frameProvider =
                    new DuplicateFrameProvider(usbSource.getCamera(), settables, config.nickname);
        } else {
            throw new IllegalArgumentException(
                    "DuplicateVisionSource currently only supports USB cameras. Source: "
                            + sourceVisionSource.getClass().getName());
        }

        // Inherit quirks from source camera
        if (config.cameraQuirks == null) {
            config.cameraQuirks = sourceVisionSource.getCameraConfiguration().cameraQuirks;
            logger.debug(
                    "Inherited camera quirks from source: "
                            + sourceVisionSource.getCameraConfiguration().cameraQuirks);
        }

        logger.info(
                "Created duplicate camera '"
                        + config.nickname
                        + "' from source '"
                        + sourceVisionSource.getCameraConfiguration().nickname
                        + "'");
    }

    /**
     * Get the source VisionSource that this duplicate wraps.
     *
     * @return The source VisionSource
     */
    public VisionSource getSourceVisionSource() {
        return sourceVisionSource;
    }

    @Override
    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return settables;
    }

    @Override
    public boolean isVendorCamera() {
        // Duplicates are never vendor cameras, even if source is
        return false;
    }

    @Override
    public boolean hasLEDs() {
        // Duplicates never control LEDs - only the source camera does
        return false;
    }

    @Override
    public void remakeSettables() {
        // Nothing to remake - settables are just a read-only wrapper
    }

    @Override
    public void release() {
        // DO NOT release the source - only release our own resources
        // The source VisionModule is still active and may have other duplicates
        logger.debug("Releasing duplicate camera: " + cameraConfiguration.nickname);
        frameProvider.release();
    }
}

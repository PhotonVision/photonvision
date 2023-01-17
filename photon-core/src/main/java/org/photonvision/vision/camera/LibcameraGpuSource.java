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

import edu.wpi.first.cscore.VideoMode;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.LibcameraGpuFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class LibcameraGpuSource extends VisionSource {
    static final Logger logger = new Logger(LibcameraGpuSource.class, LogGroup.Camera);

    private final LibcameraGpuSettables settables;
    private final LibcameraGpuFrameProvider frameProvider;

    public LibcameraGpuSource(CameraConfiguration configuration) {
        super(configuration);
        if (configuration.cameraType != CameraType.ZeroCopyPicam) {
            throw new IllegalArgumentException(
                    "GPUAcceleratedPicamSource only accepts CameraConfigurations with type Picam");
        }

        settables = new LibcameraGpuSettables(configuration);
        frameProvider = new LibcameraGpuFrameProvider(settables);
    }

    @Override
    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return settables;
    }

    /**
     * On the OV5649 the actual FPS we want to request from the GPU can be higher than the FPS that we
     * can do after processing. On the IMX219 these FPSes match pretty closely, except for the
     * 1280x720 mode. We use this to present a rated FPS to the user that's lower than the actual FPS
     * we request from the GPU. This is important for setting user expectations, and is also used by
     * the frontend to detect and explain FPS drops. This class should ONLY be used by Picam video
     * modes! This is to make sure it shows up nice in the frontend
     */
    public static class FPSRatedVideoMode extends VideoMode {
        public final int fpsActual;
        public final double fovMultiplier;

        public FPSRatedVideoMode(
                PixelFormat pixelFormat,
                int width,
                int height,
                int ratedFPS,
                int actualFPS,
                double fovMultiplier) {
            super(pixelFormat, width, height, ratedFPS);

            this.fpsActual = actualFPS;
            this.fovMultiplier = fovMultiplier;
        }
    }

    @Override
    public boolean isVendorCamera() {
        return ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV();
    }
}

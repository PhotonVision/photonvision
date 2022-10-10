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
import edu.wpi.first.cscore.VideoMode.PixelFormat;
import java.nio.file.Path;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class FileVisionSource extends VisionSource {
    private final FileFrameProvider frameProvider;
    private final FileSourceSettables settables;

    public FileVisionSource(CameraConfiguration cameraConfiguration) {
        super(cameraConfiguration);
        var calibration =
                cameraConfiguration.calibrations.size() > 0
                        ? cameraConfiguration.calibrations.get(0)
                        : null;
        frameProvider =
                new FileFrameProvider(
                        Path.of(cameraConfiguration.path),
                        cameraConfiguration.FOV,
                        FileFrameProvider.MAX_FPS,
                        calibration);
        settables =
                new FileSourceSettables(cameraConfiguration, frameProvider.get().frameStaticProperties);
    }

    public FileVisionSource(String name, String imagePath, double fov) {
        super(new CameraConfiguration(name, imagePath));
        frameProvider = new FileFrameProvider(imagePath, fov);
        settables =
                new FileSourceSettables(cameraConfiguration, frameProvider.get().frameStaticProperties);
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
        return false;
    }

    private static class FileSourceSettables extends VisionSourceSettables {
        private final VideoMode videoMode;

        private final HashMap<Integer, VideoMode> videoModes = new HashMap<>();

        FileSourceSettables(
                CameraConfiguration cameraConfiguration, FrameStaticProperties frameStaticProperties) {
            super(cameraConfiguration);
            this.frameStaticProperties = frameStaticProperties;
            videoMode =
                    new VideoMode(
                            PixelFormat.kMJPEG,
                            frameStaticProperties.imageWidth,
                            frameStaticProperties.imageHeight,
                            30);
            videoModes.put(0, videoMode);
        }

        @Override
        public void setExposure(double exposure) {}

        public void setAutoExposure(boolean cameraAutoExposure) {}

        @Override
        public void setBrightness(int brightness) {}

        @Override
        public void setGain(int gain) {}

        @Override
        public VideoMode getCurrentVideoMode() {
            return videoMode;
        }

        @Override
        protected void setVideoModeInternal(VideoMode videoMode) {
            // Do nothing
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return videoModes;
        }
    }
}

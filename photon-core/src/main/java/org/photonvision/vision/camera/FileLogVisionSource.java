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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileLogFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.wpilib.util.PixelFormat;
import org.wpilib.vision.camera.VideoMode;

/**
 * VisionSource backed by a {@link FileLogFrameProvider} — replays a recording directory
 * (recording.mp4 + metadata.jsonl) as if it were a live camera. Parallel to {@link
 * FileVisionSource}; kept separate because the lifecycles diverge (FileVisionSource loops a
 * single image, this one walks through a timed recording and exhausts).
 */
public class FileLogVisionSource extends VisionSource {
    private final FileLogFrameProvider frameProvider;
    private final FileLogSourceSettables settables;

    public FileLogVisionSource(CameraConfiguration cameraConfiguration) {
        super(cameraConfiguration);

        try {
            this.frameProvider =
                    new FileLogFrameProvider(Path.of(cameraConfiguration.getDevicePath()));
        } catch (IOException e) {
            // VisionSourceManager.loadVisionSourceFromCamConfig has no throws clause and the
            // existing switch arms don't propagate checked exceptions either. Wrap so the
            // failure surfaces in the same shape as other source-construction errors
            // (e.g. a CSI source whose device is unplugged).
            throw new RuntimeException(
                    "Failed to open replay source at " + cameraConfiguration.getDevicePath(), e);
        }

        if (getCameraConfiguration().cameraQuirks == null) {
            getCameraConfiguration().cameraQuirks = QuirkyCamera.DefaultCamera;
        }

        this.settables =
                new FileLogSourceSettables(cameraConfiguration, frameProvider.getStaticProperties());
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

    @Override
    public boolean hasLEDs() {
        return false;
    }

    @Override
    public void remakeSettables() {
        // Dimensions are baked in at construction from the mp4 header; never need to rebuild.
    }

    @Override
    public void release() {
        frameProvider.release();
    }

    /**
     * Mirrors {@code FileVisionSource.FileSourceSettables} — every camera control is a no-op
     * (exposure / gain / white balance are baked into the recording) and there's exactly one
     * video mode whose dimensions come from the mp4 header. fps is the rate {@code FrameRecorder}
     * encodes at; the provider's inline pacing honours the recording's per-frame deltas
     * regardless.
     */
    public static class FileLogSourceSettables extends VisionSourceSettables {
        private final VideoMode videoMode;
        private final HashMap<Integer, VideoMode> videoModes = new HashMap<>();

        FileLogSourceSettables(
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
        public void setExposureRaw(double exposureRaw) {}

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
            // Dimensions are baked into the recording; setting a different mode is a no-op.
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return videoModes;
        }

        @Override
        public double getMinExposureRaw() {
            return 1f;
        }

        @Override
        public double getMaxExposureRaw() {
            return 100f;
        }

        @Override
        public void setAutoWhiteBalance(boolean autowb) {}

        @Override
        public void setWhiteBalanceTemp(double temp) {}

        @Override
        public double getMaxWhiteBalanceTemp() {
            return 2;
        }

        @Override
        public double getMinWhiteBalanceTemp() {
            return 1;
        }
    }
}

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

import edu.wpi.cscore.VideoMode;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.AcceleratedPicamFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class ZeroCopyPicamSource implements VisionSource {
    private static final Logger logger = new Logger(ZeroCopyPicamSource.class, LogGroup.Camera);

    private final VisionSourceSettables settables;
    private final AcceleratedPicamFrameProvider frameProvider;

    public ZeroCopyPicamSource(CameraConfiguration configuration) {
        if (configuration.cameraType != CameraType.ZeroCopyPicam) {
            throw new IllegalArgumentException(
                    "GPUAcceleratedPicamSource only accepts CameraConfigurations with type Picam");
        }

        settables = new PicamSettables(configuration);
        frameProvider = new AcceleratedPicamFrameProvider(settables);
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
    * the frontend to detect and explain FPS drops.
    */
    private static class FPSRatedVideoMode extends VideoMode {
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

    public static class PicamSettables extends VisionSourceSettables {

        private FPSRatedVideoMode currentVideoMode;
        private double lastExposure;
        private int lastBrightness;
        private int lastGain;

        public PicamSettables(CameraConfiguration configuration) {
            super(configuration);

            videoModes = new HashMap<>();
            PicamJNI.SensorModel sensorModel = PicamJNI.getSensorModel();

            if (sensorModel == PicamJNI.SensorModel.IMX219) {
                // Settings for the IMX219 sensor, which is used on the Pi Camera Module v2
                videoModes.put(
                        0, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 320, 240, 120, 120, .39));
                videoModes.put(
                        1, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 640, 480, 65, 90, .39));
                videoModes.put(
                        2, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 1280, 720, 40, 90, .72));
                videoModes.put(
                        3, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 1920, 1080, 15, 20, .53));
            } else {
                if (sensorModel == PicamJNI.SensorModel.IMX477) {
                    logger.warn(
                            "It appears you are using a Pi HQ Camera. This camera is not officially supported. You will have to set your camera FOV differently based on resolution.");
                } else if (sensorModel == PicamJNI.SensorModel.Unknown) {
                    logger.warn(
                            "You have an unknown sensor connected to your Pi over CSI! This is likely a bug. If it is not, then you will have to set your camera FOV differently based on resolution.");
                }

                // Settings for the OV5647 sensor, which is used by the Pi Camera Module v1
                videoModes.put(
                        0, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 320, 240, 90, 90, 1));
                videoModes.put(
                        1, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 640, 480, 85, 90, 1));
                videoModes.put(
                        2, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 960, 720, 45, 60, 1));
                videoModes.put(
                        3, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 1280, 720, 30, 45, 0.92));
                videoModes.put(
                        4, new FPSRatedVideoMode(VideoMode.PixelFormat.kUnknown, 1920, 1080, 15, 20, 0.72));
            }

            currentVideoMode = (FPSRatedVideoMode) videoModes.get(0);
        }

        @Override
        public double getFOV() {
            return getCurrentVideoMode().fovMultiplier * getConfiguration().FOV;
        }

        @Override
        public void setExposure(double exposure) {
            lastExposure = exposure;
            PicamJNI.setExposure((int) Math.round(exposure));
        }

        @Override
        public void setBrightness(int brightness) {
            lastBrightness = brightness;
            PicamJNI.setBrightness(brightness);
        }

        @Override
        public void setGain(int gain) {
            lastGain = gain;
            PicamJNI.setGain(gain);
        }

        @Override
        public FPSRatedVideoMode getCurrentVideoMode() {
            return currentVideoMode;
        }

        @Override
        protected void setVideoModeInternal(VideoMode videoMode) {
            var mode = (FPSRatedVideoMode) videoMode;
            PicamJNI.destroyCamera();
            PicamJNI.createCamera(mode.width, mode.height, mode.fpsActual);

            // We don't store last settings on the native side, and when you change video mode these get
            // reset on MMAL's end
            setExposure(lastExposure);
            setBrightness(lastBrightness);
            setGain(lastGain);

            currentVideoMode = mode;
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return videoModes;
        }
    }

    @Override
    public boolean isVendorCamera() {
        return ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV();
    }
}

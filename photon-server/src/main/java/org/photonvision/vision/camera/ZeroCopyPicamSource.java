/*
 * Copyright (C) 2020 Photon Vision.
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
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.AcceleratedPicamFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class ZeroCopyPicamSource implements VisionSource {

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

        public FPSRatedVideoMode(
                PixelFormat pixelFormat, int width, int height, int ratedFPS, int actualFPS) {
            super(pixelFormat, width, height, ratedFPS);

            this.fpsActual = actualFPS;
        }
    }

    public static class PicamSettables extends VisionSourceSettables {

        private VideoMode currentVideoMode;
        private double lastExposure;
        private int lastBrightness;
        private int lastGain;

        public PicamSettables(CameraConfiguration configuration) {
            super(configuration);

            videoModes = new HashMap<>();
            videoModes.put(
                    0,
                    new FPSRatedVideoMode(
                            VideoMode.PixelFormat.kUnknown, 320, 240, 90, 90)); // Was 120 on IMX219
            videoModes.put(
                    1,
                    new FPSRatedVideoMode(
                            VideoMode.PixelFormat.kUnknown, 640, 480, 85, 90)); // Was 65-70 on IMX219
            videoModes.put(
                    2,
                    new FPSRatedVideoMode(
                            VideoMode.PixelFormat.kUnknown, 960, 720, 45, 60)); // Was 45 on IMX219
            videoModes.put(
                    3,
                    new FPSRatedVideoMode(
                            VideoMode.PixelFormat.kUnknown, 1280, 720, 30, 45)); // Was 40 on IMX219
            videoModes.put(
                    4,
                    new FPSRatedVideoMode(
                            VideoMode.PixelFormat.kUnknown, 1920, 1080, 15, 20)); // Was 15 on IMX219

            currentVideoMode = videoModes.get(0);
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
        public VideoMode getCurrentVideoMode() {
            return currentVideoMode;
        }

        @Override
        protected void setVideoModeInternal(VideoMode videoMode) {
            PicamJNI.destroyCamera();
            PicamJNI.createCamera(
                    videoMode.width, videoMode.height, ((FPSRatedVideoMode) videoMode).fpsActual);

            // We don't store last settings on the native side, and when you change video mode these get
            // reset on MMAL's end
            setExposure(lastExposure);
            setBrightness(lastBrightness);
            setGain(lastGain);

            currentVideoMode = videoMode;
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

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

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.*;
import java.util.*;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.USBFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBCameraSource extends VisionSource {
    private final Logger logger;
    private final UsbCamera camera;
    private final USBCameraSettables usbCameraSettables;
    private final USBFrameProvider usbFrameProvider;
    private final CvSink cvSink;

    public final QuirkyCamera cameraQuirks;

    public USBCameraSource(CameraConfiguration config) {
        super(config);

        logger = new Logger(USBCameraSource.class, config.nickname, LogGroup.Camera);
        camera = new UsbCamera(config.nickname, config.path);
        cvSink = CameraServer.getVideo(this.camera);

        cameraQuirks =
                QuirkyCamera.getQuirkyCamera(
                        camera.getInfo().productId, camera.getInfo().vendorId, config.baseName);

        if (cameraQuirks.hasQuirks()) {
            logger.info("Quirky camera detected: " + cameraQuirks.baseName);
        }

        if (cameraQuirks.hasQuirk(CameraQuirk.CompletelyBroken)) {
            // set some defaults, as these should never be used.
            logger.info("Camera " + cameraQuirks.baseName + " is not supported for PhotonVision");
            usbCameraSettables = null;
            usbFrameProvider = null;
        } else {
            // Normal init
            // auto exposure/brightness/gain will be set by the visionmodule later
            disableAutoFocus();

            usbCameraSettables = new USBCameraSettables(config);
            if (usbCameraSettables.getAllVideoModes().isEmpty()) {
                logger.info("Camera " + camera.getPath() + " has no video modes supported by PhotonVision");
                usbFrameProvider = null;
            } else {
                usbFrameProvider = new USBFrameProvider(cvSink, usbCameraSettables);
            }
        }
    }

    void disableAutoFocus() {
        if (cameraQuirks.hasQuirk(CameraQuirk.AdjustableFocus)) {
            try {
                camera.getProperty("focus_auto").set(0);
                camera.getProperty("focus_absolute").set(0); // Focus into infinity
            } catch (VideoException e) {
                logger.error("Unable to disable autofocus!", e);
            }
        }
    }

    @Override
    public FrameProvider getFrameProvider() {
        return usbFrameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return this.usbCameraSettables;
    }

    public class USBCameraSettables extends VisionSourceSettables {
        protected USBCameraSettables(CameraConfiguration configuration) {
            super(configuration);
            getAllVideoModes();
            setVideoMode(videoModes.get(0));
        }

        public void setAutoExposure(boolean cameraAutoExposure) {
            logger.debug("Setting auto exposure to " + cameraAutoExposure);

            if (cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                // Case, we know this is a picam. Go through v4l2-ctl interface directly

                // Common settings
                camera
                        .getProperty("image_stabilization")
                        .set(0); // No image stabilization, as this will throw off odometry
                camera.getProperty("power_line_frequency").set(2); // Assume 60Hz USA
                camera.getProperty("scene_mode").set(0); // no presets
                camera.getProperty("exposure_metering_mode").set(0);
                camera.getProperty("exposure_dynamic_framerate").set(0);

                if (!cameraAutoExposure) {
                    // Pick a bunch of reasonable setting defaults for vision processing retroreflective
                    camera.getProperty("auto_exposure_bias").set(0);
                    camera.getProperty("iso_sensitivity_auto").set(0); // Disable auto ISO adjustement
                    camera.getProperty("iso_sensitivity").set(0); // Manual ISO adjustement
                    camera.getProperty("white_balance_auto_preset").set(2); // Auto white-balance disabled
                    camera.getProperty("auto_exposure").set(1); // auto exposure disabled
                } else {
                    // Pick a bunch of reasonable setting defaults for driver, fiducials, or otherwise
                    // nice-for-humans
                    camera.getProperty("auto_exposure_bias").set(12);
                    camera.getProperty("iso_sensitivity_auto").set(1);
                    camera.getProperty("iso_sensitivity").set(1); // Manual ISO adjustement by default
                    camera.getProperty("white_balance_auto_preset").set(1); // Auto white-balance enabled
                    camera.getProperty("auto_exposure").set(0); // auto exposure enabled
                }

            } else {
                // Case - this is some other USB cam. Default to wpilib's implementation

                var canSetWhiteBalance = !cameraQuirks.hasQuirk(CameraQuirk.Gain);

                if (!cameraAutoExposure) {
                    // Pick a bunch of reasonable setting defaults for vision processing retroreflective
                    if (canSetWhiteBalance) {
                        camera.setWhiteBalanceManual(4000); // Auto white-balance disabled, 4000K preset
                    }
                } else {
                    // Pick a bunch of reasonable setting defaults for driver, fiducials, or otherwise
                    // nice-for-humans
                    if (canSetWhiteBalance) {
                        camera.setWhiteBalanceAuto(); // Auto white-balance enabled
                    }
                    camera.setExposureAuto(); // auto exposure enabled
                }
            }
        }

        private int timeToPiCamRawExposure(double time_us) {
            int retVal =
                    (int)
                            Math.round(
                                    time_us / 100.0); // Pi Cam's (both v1 and v2) need exposure time in units of
            // 100us/bit
            return Math.min(Math.max(retVal, 1), 10000); // Cap to allowable range for parameter
        }

        private double pctToExposureTimeUs(double pct_in) {
            // Mirror the photonvision raspicam driver's algorithm for picking an exposure time
            // from a 0-100% input
            final double PADDING_LOW_US = 10;
            final double PADDING_HIGH_US = 10;
            return PADDING_LOW_US
                    + (pct_in / 100.0) * ((1e6 / (double) camera.getVideoMode().fps) - PADDING_HIGH_US);
        }

        @Override
        public void setExposure(double exposure) {
            if (exposure >= 0.0) {
                try {
                    int scaledExposure = 1;
                    if (cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                        scaledExposure =
                                (int) Math.round(timeToPiCamRawExposure(pctToExposureTimeUs(exposure)));
                        logger.debug("Setting camera raw exposure to " + Integer.toString(scaledExposure));
                        camera.getProperty("raw_exposure_time_absolute").set(scaledExposure);
                        camera.getProperty("raw_exposure_time_absolute").set(scaledExposure);

                    } else {
                        scaledExposure = (int) Math.round(exposure);
                        logger.debug("Setting camera exposure to " + Integer.toString(scaledExposure));
                        camera.setExposureManual(scaledExposure);
                        camera.setExposureManual(scaledExposure);
                    }
                } catch (VideoException e) {
                    logger.error("Failed to set camera exposure!", e);
                }
            }
        }

        @Override
        public void setBrightness(int brightness) {
            try {
                camera.setBrightness(brightness);
                camera.setBrightness(brightness);
            } catch (VideoException e) {
                logger.error("Failed to set camera brightness!", e);
            }
        }

        @Override
        public void setGain(int gain) {
            try {
                if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
                    camera.getProperty("gain_automatic").set(0);
                    camera.getProperty("gain").set(gain);
                }
            } catch (VideoException e) {
                logger.error("Failed to set camera gain!", e);
            }
        }

        @Override
        public VideoMode getCurrentVideoMode() {
            return camera.isConnected() ? camera.getVideoMode() : null;
        }

        @Override
        public void setVideoModeInternal(VideoMode videoMode) {
            try {
                if (videoMode == null) {
                    logger.error("Got a null video mode! Doing nothing...");
                    return;
                }
                camera.setVideoMode(videoMode);
            } catch (Exception e) {
                logger.error("Failed to set video mode!", e);
            }
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            if (videoModes == null) {
                videoModes = new HashMap<>();
                List<VideoMode> videoModesList = new ArrayList<>();
                try {
                    VideoMode[] modes;
                    if (cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                        modes =
                                new VideoMode[] {
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 320, 240, 90),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 320, 240, 30),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 320, 240, 15),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 320, 240, 10),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 640, 480, 90),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 640, 480, 45),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 640, 480, 30),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 640, 480, 15),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 640, 480, 10),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 960, 720, 60),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 960, 720, 10),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 1280, 720, 45),
                                    new VideoMode(VideoMode.PixelFormat.kBGR, 1920, 1080, 20),
                                };
                    } else {
                        modes = camera.enumerateVideoModes();
                    }
                    for (int i = 0; i < modes.length; i++) {
                        var videoMode = modes[i];

                        // Filter grey modes
                        if (videoMode.pixelFormat == VideoMode.PixelFormat.kGray
                                || videoMode.pixelFormat == VideoMode.PixelFormat.kUnknown) {
                            continue;
                        }

                        // On picam, filter non-bgr modes for performance
                        if (cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            if (videoMode.pixelFormat != VideoMode.PixelFormat.kBGR) {
                                continue;
                            }
                        }

                        if (cameraQuirks.hasQuirk(CameraQuirk.FPSCap100)) {
                            if (videoMode.fps > 100) {
                                continue;
                            }
                        }

                        videoModesList.add(videoMode);

                        // TODO - do we want to trim down FPS modes? in cases where the camera has no gain
                        // control,
                        // lower FPS might be needed to ensure total exposure is acceptable.
                        // We look for modes with the same height/width/pixelformat as this mode
                        // and remove all the ones that are slower. This is sorted low to high.
                        // So we remove the last element (the fastest FPS) from the duplicate list,
                        // and remove all remaining elements from the final list
                        // var duplicateModes =
                        //         videoModesList.stream()
                        //                 .filter(
                        //                         it ->
                        //                                 it.height == videoMode.height
                        //                                         && it.width == videoMode.width
                        //                                         && it.pixelFormat == videoMode.pixelFormat)
                        //                 .sorted(Comparator.comparingDouble(it -> it.fps))
                        //                 .collect(Collectors.toList());
                        // duplicateModes.remove(duplicateModes.size() - 1);
                        // videoModesList.removeAll(duplicateModes);
                    }
                } catch (Exception e) {
                    logger.error("Exception while enumerating video modes!", e);
                    videoModesList = List.of();
                }

                // Sort by resolution
                var sortedList =
                        videoModesList.stream()
                                .sorted(((a, b) -> (b.width + b.height) - (a.width + a.height)))
                                .collect(Collectors.toList());
                Collections.reverse(sortedList);

                // On vendor cameras, respect blacklisted indices
                var indexBlacklist =
                        ConfigManager.getInstance().getConfig().getHardwareConfig().blacklistedResIndices;
                for (int badIdx : indexBlacklist) {
                    sortedList.remove(badIdx);
                }

                for (VideoMode videoMode : sortedList) {
                    videoModes.put(sortedList.indexOf(videoMode), videoMode);
                }
            }
            return videoModes;
        }
    }

    // TODO improve robustness of this detection
    @Override
    public boolean isVendorCamera() {
        return ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV()
                && cameraQuirks.hasQuirk(CameraQuirk.PiCam);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        USBCameraSource that = (USBCameraSource) o;
        return cameraQuirks.equals(that.cameraQuirks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                camera, usbCameraSettables, usbFrameProvider, cameraConfiguration, cvSink, cameraQuirks);
    }
}

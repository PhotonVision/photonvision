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
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.cscore.VideoProperty;
import edu.wpi.first.util.PixelFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.frame.provider.USBFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBCameraSource extends VisionSource {
    private final Logger logger;
    private final UsbCamera camera;
    protected USBCameraSettables usbCameraSettables;
    protected FrameProvider usbFrameProvider;
    private final CvSink cvSink;

    private VideoProperty exposureAbsProp = null;
    private VideoProperty autoExposureProp = null;
    private double minExposure = 1;
    private double maxExposure = 80000;

    private int PROP_AUTO_EXPOSURE_ENABLED = 3;
    private int PROP_AUTO_EXPOSURE_DISABLED = 1;

    public USBCameraSource(CameraConfiguration config) {
        super(config);

        logger = new Logger(USBCameraSource.class, config.nickname, LogGroup.Camera);
        // cscore will auto-reconnect to the camera path we give it. v4l does not guarantee that if i
        // swap cameras around, the same /dev/videoN ID will be assigned to that camera. So instead
        // default to pinning to a particular USB port, or by "path" (appears to be a global identifier)
        // on Windows.
        camera = new UsbCamera(config.nickname, config.getUSBPath().orElse(config.path));
        cvSink = CameraServer.getVideo(this.camera);

        // set vid/pid if not done already for future matching
        if (config.usbVID <= 0) config.usbVID = this.camera.getInfo().vendorId;
        if (config.usbPID <= 0) config.usbPID = this.camera.getInfo().productId;

        if (getCameraConfiguration().cameraQuirks == null) {
            getCameraConfiguration().cameraQuirks =
                    QuirkyCamera.getQuirkyCamera(
                            camera.getInfo().vendorId, camera.getInfo().productId, config.baseName);
        }

        if (getCameraConfiguration().cameraQuirks.hasQuirks()) {
            logger.info("Quirky camera detected: " + getCameraConfiguration().cameraQuirks.baseName);
        }

        // Aid to the development team - record the properties available for whatever the user plugged
        // in
        printCameraProperaties();

        // Photonvision needs to be able to control absolute exposure. Make sure we can first.
        var expProp = findProperty("raw_exposure_absolute", "raw_exposure_time_absolute", "exposure");

        // Photonvision needs to be able to control auto exposure. Make sure we can first.
        var autoExpProp = findProperty("exposure_auto", "auto_exposure");

        var cameraBroken =
                getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.CompletelyBroken)
                        || expProp.isEmpty()
                        || autoExpProp.isEmpty();

        if (cameraBroken) {
            // Known issues - Disable this camera
            logger.info(
                    "Camera "
                            + getCameraConfiguration().cameraQuirks.baseName
                            + " is not supported for PhotonVision");
            // set some defaults, as these should never be used.
            usbCameraSettables = null;
            usbFrameProvider = null;

        } else {
            // Camera is likely to work, set up the Settables
            usbCameraSettables = new USBCameraSettables(config);

            if (usbCameraSettables.getAllVideoModes().isEmpty()) {
                // No video modes produced from settables, disable the camera
                logger.info("Camera " + camera.getPath() + " has no video modes supported by PhotonVision");
                usbFrameProvider = null;

            } else {
                // Functional camera, set up the frame provider and configure defaults
                usbFrameProvider = new USBFrameProvider(cvSink, usbCameraSettables);
                setAllCamDefaults();
                exposureAbsProp  = expProp.get();
                autoExposureProp = autoExpProp.get();

                this.minExposure = exposureAbsProp.getMin();
                this.maxExposure = exposureAbsProp.getMax();

                if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.ArduOV2311)) {
                    // Property limits are incorrect
                    this.minExposure = 1;
                    this.maxExposure = 75;
                }

                if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.LifeCamExposure)) {
                    // Camera seems unstable above this point.
                    this.maxExposure = 750;
                }

                if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.OneZeroAutoExposure)) {
                    PROP_AUTO_EXPOSURE_ENABLED = 0;
                    PROP_AUTO_EXPOSURE_DISABLED = 1;
                }
            }
        }
    }

    /**
     * Returns the first property with a name in the list. Useful to find gandolf property that goes
     * by many names in different os/releases/whatever
     *
     * @param options
     * @return
     */
    private Optional<VideoProperty> findProperty(String... options) {
        VideoProperty retProp = null;
        boolean found = false;
        for (var option : options) {
            retProp = camera.getProperty(option);
            if (retProp.getKind() != VideoProperty.Kind.kNone) {
                // got em
                found = true;
                break;
            }
        }

        if (!found) {
            logger.warn(
                    "Expected at least one of the following properties to be available: "
                            + Arrays.toString(options));
            retProp = null;
        }

        return Optional.ofNullable(retProp);
    }

    /**
     * Forgiving "set this property" action. Produces a debug message but skips properties if they
     * aren't supported Errors if the property exists but the set fails.
     *
     * @param property
     * @param value
     */
    private void softSet(String property, int value) {
        VideoProperty prop = camera.getProperty(property);
        if (prop.getKind() == VideoProperty.Kind.kNone) {
            logger.debug("No property " + property + " for " + camera.getName() + " , skipping.");
        } else {
            try {
                prop.set(value);
            } catch (VideoException e) {
                logger.error("Failed to set " + property + " for " + camera.getName() + " !", e);
            }
        }
    }

    private void printCameraProperaties() {

        VideoProperty[] cameraProperties = null;
        try {
            cameraProperties = camera.enumerateProperties();
        } catch (VideoException e){
            logger.error("Failed to list camera properties!", e);
        }

        if(cameraProperties != null){
            String cameraPropertiesStr = "Cam Properties Dump:\n";
            for (int i = 0; i < cameraProperties.length; i++) {
                cameraPropertiesStr +=
                        "Name: "
                                + cameraProperties[i].getName()
                                + ", Kind: "
                                + cameraProperties[i].getKind()
                                + ", Value: "
                                + cameraProperties[i].getKind().getValue()
                                + ", Min: "
                                + cameraProperties[i].getMin()
                                + ", Max: "
                                + cameraProperties[i].getMax()
                                + ", Dflt: "
                                + cameraProperties[i].getDefault()
                                + ", Step: "
                                + cameraProperties[i].getStep()
                                + "\n";
            }
            logger.debug(cameraPropertiesStr);
        }
    }

    private void setAllCamDefaults() {
        // Common settings for all cameras to attempt to get their image
        // as close as possible to what we want for image processing
        softSet("image_stabilization", 0); // No image stabilization, as this will throw off odometry
        softSet("power_line_frequency", 2); // Assume 60Hz USA
        softSet("scene_mode", 0); // no presets
        softSet("exposure_metering_mode", 0);
        softSet("exposure_dynamic_framerate", 0);
        softSet("focus_auto", 0);
        softSet("focus_absolute", 0); // Focus into infinity
    }

    public QuirkyCamera getCameraQuirks() {
        return getCameraConfiguration().cameraQuirks;
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
        // We need to remember the last exposure set when exiting
        // auto exposure mode so we can restore it
        private double lastExposureRaw = -1;

        // Some cameras need logic where we re-apply brightness after
        // changing exposure
        private int lastBrightness = -1;

        public USBCameraSettables(CameraConfiguration configuration) {
            super(configuration);
            getAllVideoModes();
            if (!configuration.cameraQuirks.hasQuirk(CameraQuirk.StickyFPS))
                if (!videoModes.isEmpty()) setVideoMode(videoModes.get(0)); // fixes double FPS set
        }

        public void setAutoExposure(boolean cameraAutoExposure) {

            logger.debug("Setting auto exposure to " + cameraAutoExposure);

            if (!cameraAutoExposure) {
                // Pick a bunch of reasonable setting defaults for vision processing
                softSet("auto_exposure_bias", 0);
                softSet("iso_sensitivity_auto", 0); // Disable auto ISO adjustment
                softSet("iso_sensitivity", 0); // Manual ISO adjustment
                softSet("white_balance_auto_preset", 2); // Auto white-balance disabled
                softSet("white_balance_automatic", 0);
                softSet("white_balance_temperature", 4000);
                autoExposureProp.set(PROP_AUTO_EXPOSURE_ENABLED);

                // Most cameras leave exposure time absolute at the last value from their AE algorithm.
                // Set it back to the exposure slider value
                setExposureRaw(this.lastExposureRaw);

            } else {
                // Pick a bunch of reasonable setting to make the picture nice-for-humans
                softSet("auto_exposure_bias", 12);
                softSet("iso_sensitivity_auto", 1);
                softSet("iso_sensitivity", 1); // Manual ISO adjustment by default
                softSet("white_balance_auto_preset", 1); // Auto white-balance enabled
                softSet("white_balance_automatic", 1);
                autoExposureProp.set(PROP_AUTO_EXPOSURE_DISABLED);
            }
        }

        @Override
        public double getMinExposureRaw() {
            return minExposure;
        }

        @Override
        public double getMaxExposureRaw() {
            return maxExposure;
        }

        @Override
        public void setExposureRaw(double exposureRaw) {
            if (exposureRaw >= 0.0) {
                try {
                    autoExposureProp.set(PROP_AUTO_EXPOSURE_DISABLED);

                    int propVal = (int) MathUtils.limit(exposureRaw, minExposure, maxExposure);

                    if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.LifeCamExposure)) {
                        // Lifecam only allows certain settings for exposure
                        propVal = MathUtils.quantize(propVal, CameraQuirkConstants.LifecamAllowableExposures);
                    }

                    logger.debug(
                            "Setting property "
                                    + autoExposureProp.getName()
                                    + " to "
                                    + propVal
                                    + " (user requested "
                                    + exposureRaw
                                    + " μs)");

                    exposureAbsProp.set(propVal);

                    this.lastExposureRaw = exposureRaw;

                    if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.LifeCamExposure)) {
                        // Lifecam requires setting brightness again after exposure
                        // And it requires setting it twice, ensuring the value is different
                        // This camera is very bork.
                        if (lastBrightness >= 0) {
                            setBrightness(lastBrightness - 1);
                        }
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
                this.lastBrightness = brightness;
            } catch (VideoException e) {
                logger.error("Failed to set camera brightness!", e);
            }
        }

        @Override
        public void setGain(int gain) {
            softSet("gain_automatic", 0);
            softSet("gain", gain);
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

                    modes = camera.enumerateVideoModes();

                    for (VideoMode videoMode : modes) {
                        // Filter grey modes
                        if (videoMode.pixelFormat == PixelFormat.kGray
                                || videoMode.pixelFormat == PixelFormat.kUnknown) {
                            continue;
                        }

                        if (getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.FPSCap100)) {
                            if (videoMode.fps > 100) {
                                continue;
                            }
                        }

                        videoModesList.add(videoMode);
                    }
                } catch (Exception e) {
                    logger.error("Exception while enumerating video modes!", e);
                    videoModesList = List.of();
                }

                // Sort by resolution
                var sortedList =
                        videoModesList.stream()
                                .distinct() // remove redundant video mode entries
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

    @Override
    public boolean isVendorCamera() {
        return false; // Vendors do not supply USB Cameras
    }

    @Override
    public boolean hasLEDs() {
        return false; // Assume USB cameras do not have photonvision-controlled LEDs
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        USBCameraSource other = (USBCameraSource) obj;
        if (camera == null) {
            if (other.camera != null) return false;
        } else if (!camera.equals(other.camera)) return false;
        if (usbCameraSettables == null) {
            if (other.usbCameraSettables != null) return false;
        } else if (!usbCameraSettables.equals(other.usbCameraSettables)) return false;
        if (usbFrameProvider == null) {
            if (other.usbFrameProvider != null) return false;
        } else if (!usbFrameProvider.equals(other.usbFrameProvider)) return false;
        if (cvSink == null) {
            if (other.cvSink != null) return false;
        } else if (!cvSink.equals(other.cvSink)) return false;
        if (getCameraConfiguration().cameraQuirks == null) {
            if (other.getCameraConfiguration().cameraQuirks != null) return false;
        } else if (!getCameraConfiguration()
                .cameraQuirks
                .equals(other.getCameraConfiguration().cameraQuirks)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                camera,
                usbCameraSettables,
                usbFrameProvider,
                cameraConfiguration,
                cvSink,
                getCameraConfiguration().cameraQuirks);
    }
}

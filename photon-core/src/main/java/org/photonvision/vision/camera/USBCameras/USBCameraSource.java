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

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.cscore.VideoProperty;
import edu.wpi.first.util.RuntimeDetector;
import java.util.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.USBFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBCameraSource extends VisionSource {
    private final Logger logger;
    private final UsbCamera camera;
    protected GenericUSBCameraSettables settables;
    protected FrameProvider usbFrameProvider;
    private final CvSink cvSink;

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

        var cameraBroken = getCameraConfiguration().cameraQuirks.hasQuirk(CameraQuirk.CompletelyBroken);

        if (cameraBroken) {
            // Known issues - Disable this camera
            logger.info(
                    "Camera "
                            + getCameraConfiguration().cameraQuirks.baseName
                            + " is not supported for PhotonVision");
            // set some defaults, as these should never be used.
            settables = null;
            usbFrameProvider = null;

        } else {
            // Camera is likely to work, set up the Settables
            settables = createSettables(config, camera);

            if (settables.getAllVideoModes().isEmpty()) {
                // No video modes produced from settables, disable the camera
                logger.info("Camera " + camera.getPath() + " has no video modes supported by PhotonVision");
                usbFrameProvider = null;

            } else {
                // Functional camera, set up the frame provider and configure defaults
                usbFrameProvider = new USBFrameProvider(cvSink, settables);
                settables.setAllCamDefaults();
            }
        }
    }

    /**
     * Factory for making appropriate settables
     *
     * @param config
     * @param camera
     * @return
     */
    protected GenericUSBCameraSettables createSettables(
            CameraConfiguration config, UsbCamera camera) {
        var quirks = getCameraConfiguration().cameraQuirks;

        GenericUSBCameraSettables settables;

        if (quirks.hasQuirk(CameraQuirk.LifeCamControls)) {
            if (RuntimeDetector.isWindows()) {
                logger.debug("Using Microsoft Lifecam 3000 Windows-Specific Settables");
                settables = new LifeCam3kWindowsCameraSettables(config, camera);
            } else {
                logger.debug("Using Microsoft Lifecam 3000 Settables");
                settables = new LifeCam3kCameraSettables(config, camera);
            }
        } else if (quirks.hasQuirk(CameraQuirk.PsEyeControls)) {
            logger.debug("Using PlayStation Eye Camera Settables");
            settables = new PsEyeCameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.ArduOV2311Controls)) {
            logger.debug("Using Arducam OV2311 Settables");
            settables = new ArduOV2311CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.ArduOV9281Controls)) {
            logger.debug("Using Arducam OV9281 Settables");
            settables = new InnoOV9281CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.ArduOV9782Controls)) {
            logger.debug("Using Arducam OV9782 Settables");
            settables = new ArduOV9782CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.InnoOV9281Controls)) {
            settables = new InnoOV9281CameraSettables(config, camera);
        } else {
            logger.debug("Using Generic USB Cam Settables");
            settables = new GenericUSBCameraSettables(config, camera);
        }

        settables.setUpExposureProperties();

        return settables;
    }

    /**
     * Must be called after createSettables Using the current config/camera and modified quirks, make
     * a new settables
     */
    public void remakeSettables() {
        var oldConfig = this.cameraConfiguration;
        var oldCamera = this.camera;

        this.settables = createSettables(oldConfig, oldCamera);
    }

    private void printCameraProperaties() {
        VideoProperty[] cameraProperties = null;
        try {
            cameraProperties = camera.enumerateProperties();
        } catch (VideoException e) {
            logger.error("Failed to list camera properties!", e);
        }

        if (cameraProperties != null) {
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

    public QuirkyCamera getCameraQuirks() {
        return getCameraConfiguration().cameraQuirks;
    }

    @Override
    public FrameProvider getFrameProvider() {
        return usbFrameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return this.settables;
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
        if (settables == null) {
            if (other.settables != null) return false;
        } else if (!settables.equals(other.settables)) return false;
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
                settables,
                usbFrameProvider,
                cameraConfiguration,
                cvSink,
                getCameraConfiguration().cameraQuirks);
    }
}

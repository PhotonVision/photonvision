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
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.cscore.VideoProperty;
import java.util.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.PVCameraInfo.PVUsbCameraInfo;
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

    private void onCameraConnected() {
        // Aid to the development team - record the properties available for whatever the user plugged
        // in
        printCameraProperaties();

        settables.onCameraConnected();
    }

    public USBCameraSource(CameraConfiguration config) {
        super(config);

        logger = new Logger(USBCameraSource.class, config.nickname, LogGroup.Camera);

        if (!(config.matchedCameraInfo instanceof PVUsbCameraInfo)) {
            logger.error(
                    "USBCameraSource matched to a non-USB camera info?? "
                            + config.matchedCameraInfo.toString());
        }

        camera = new UsbCamera(config.nickname, config.getDevicePath());

        // TODO - I don't need this, do I?
        // // set vid/pid if not done already for future matching
        // if (config.usbVID <= 0) config.usbVID = this.camera.getInfo().vendorId;
        // if (config.usbPID <= 0) config.usbPID = this.camera.getInfo().productId;

        // TODO - why do we delegate this to USBCameraSource? Quirks are part of the CameraConfig??
        // also TODO - is the config's saved usb info a reasonable guess for quirk detection? seems like
        // yes to me...
        if (getCameraConfiguration().cameraQuirks == null) {
            int vid =
                    (config.matchedCameraInfo instanceof PVUsbCameraInfo)
                            ? ((PVUsbCameraInfo) config.matchedCameraInfo).vendorId
                            : -1;
            int pid =
                    (config.matchedCameraInfo instanceof PVUsbCameraInfo)
                            ? ((PVUsbCameraInfo) config.matchedCameraInfo).productId
                            : -1;

            getCameraConfiguration().cameraQuirks =
                    QuirkyCamera.getQuirkyCamera(vid, pid, config.matchedCameraInfo.name());
        }

        if (getCameraConfiguration().cameraQuirks.hasQuirks()) {
            logger.info("Quirky camera detected: " + getCameraConfiguration().cameraQuirks);
        }

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
            logger.info("Created settables " + settables);

            usbFrameProvider = new USBFrameProvider(camera, settables, this::onCameraConnected);
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
            if (Platform.isWindows()) {
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
            if (Platform.isWindows()) {
                logger.debug("Using Arducam OV2311 Windows-Specific Settables");
                settables = new ArduOV2311WindowsCameraSettables(config, camera);
            } else {
                logger.debug("Using Arducam OV2311 Settables");
                settables = new ArduOV2311CameraSettables(config, camera);
            }
        } else if (quirks.hasQuirk(CameraQuirk.ArduOV9281Controls)) {
            logger.debug("Using Arducam OV9281 Settables");
            settables = new InnoOV9281CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.ArduOV9782Controls)) {
            logger.debug("Using Arducam OV9782 Settables");
            settables = new ArduOV9782CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.InnoOV9281Controls)) {
            settables = new InnoOV9281CameraSettables(config, camera);
        } else if (quirks.hasQuirk(CameraQuirk.See3Cam_24CUG)) {
            settables = new See3Cam24CUGSettables(config, camera);
        } else {
            logger.debug("Using Generic USB Cam Settables");
            settables = new GenericUSBCameraSettables(config, camera);
        }

        return settables;
    }

    /**
     * Must be called after createSettables Using the current config/camera and modified quirks, make
     * a new settables
     */
    public void remakeSettables() {
        var oldConfig = this.cameraConfiguration;
        var oldCamera = this.camera;

        // Re-create settables
        var oldVideoMode = this.settables.getCurrentVideoMode();
        this.settables = createSettables(oldConfig, oldCamera);

        // Settables only cache videomodes on connect - force this to happen next tick
        if (settables.camera.isConnected()) {
            this.settables.onCameraConnected();
        } else {
            this.usbFrameProvider.cameraPropertiesCached = false;
        }

        // And update the settables' FrameStaticProps
        settables.setVideoMode(oldVideoMode);

        // Propagate our updated settables over to the frame provider
        ((USBFrameProvider) this.usbFrameProvider).updateSettables(this.settables);
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
    public void release() {
        CameraServer.removeCamera(camera.getName());
        camera.close();
        usbFrameProvider.release();
        usbFrameProvider = null;
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
                getCameraConfiguration().cameraQuirks);
    }
}

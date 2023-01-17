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

package org.photonvision.vision.processes;

import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.UsbCameraInfo;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.LibcameraGpuSource;
import org.photonvision.vision.camera.USBCameraSource;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);
    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    final List<UsbCameraInfo> knownUsbCameras = new CopyOnWriteArrayList<>();
    final List<CameraConfiguration> unmatchedLoadedConfigs = new CopyOnWriteArrayList<>();
    private boolean hasWarned;

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    VisionSourceManager() {}

    public void registerTimedTask() {
        TimedTaskManager.getInstance().addTask("VisionSourceManager", this::tryMatchUSBCams, 3000);
    }

    public void registerLoadedConfigs(CameraConfiguration... configs) {
        registerLoadedConfigs(Arrays.asList(configs));
    }

    /**
     * Register new camera configs loaded from disk. This will add them to the list of configs to try
     * to match, and also automatically spawn new vision processes as necessary.
     *
     * @param configs The loaded camera configs.
     */
    public void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        unmatchedLoadedConfigs.addAll(configs);
    }

    protected Supplier<List<UsbCameraInfo>> cameraInfoSupplier =
            () -> List.of(UsbCamera.enumerateUsbCameras());

    protected void tryMatchUSBCams() {
        var visionSourceList = tryMatchUSBCamImpl();
        if (visionSourceList == null) return;

        logger.info("Adding " + visionSourceList.size() + " configs to VMM.");
        ConfigManager.getInstance().addCameraConfigurations(visionSourceList);
        var addedSources = VisionModuleManager.getInstance().addSources(visionSourceList);
        addedSources.forEach(VisionModule::start);
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));
    }

    protected List<VisionSource> tryMatchUSBCamImpl() {
        return tryMatchUSBCamImpl(true);
    }

    protected List<VisionSource> tryMatchUSBCamImpl(boolean createSources) {
        // Detect cameras using CSCore
        List<UsbCameraInfo> connectedCameras =
                new ArrayList<>(filterAllowedDevices(cameraInfoSupplier.get()));

        // Remove all known devices
        var notYetLoadedCams = new ArrayList<UsbCameraInfo>();
        for (var connectedCam : connectedCameras) {
            boolean cameraIsUnknown = true;
            for (UsbCameraInfo knownCam : this.knownUsbCameras) {
                if (usbCamEquals(knownCam, connectedCam)) {
                    cameraIsUnknown = false;
                    break;
                }
            }
            if (cameraIsUnknown) {
                notYetLoadedCams.add(connectedCam);
            }
        }

        if (notYetLoadedCams.isEmpty()) return null;

        if (connectedCameras.isEmpty()) {
            logger.warn(
                    "No USB cameras were detected! Check that all cameras are connected, and that the path is correct.");
            return null;
        }
        logger.debug("Matching " + notYetLoadedCams.size() + " new cameras!");

        // Sort out just the USB cams
        var usbCamConfigs = new ArrayList<>();
        for (var config : unmatchedLoadedConfigs) {
            if (config.cameraType == CameraType.UsbCamera) usbCamConfigs.add(config);
        }

        // Debug prints
        for (var info : notYetLoadedCams) {
            logger.info("Adding local video device - \"" + info.name + "\" at \"" + info.path + "\"");
        }

        if (!usbCamConfigs.isEmpty())
            logger.debug("Trying to match " + usbCamConfigs.size() + " unmatched configs...");

        // Match camera configs to physical cameras
        var matchedCameras = matchUSBCameras(notYetLoadedCams, unmatchedLoadedConfigs);
        unmatchedLoadedConfigs.removeAll(matchedCameras);
        if (!unmatchedLoadedConfigs.isEmpty() && !hasWarned) {
            logger.warn(
                    () ->
                            "After matching, "
                                    + unmatchedLoadedConfigs.size()
                                    + " configs remained unmatched. Is your camera disconnected?");
            logger.warn(
                    "Unloaded configs: "
                            + unmatchedLoadedConfigs.stream()
                                    .map(it -> it.nickname)
                                    .collect(Collectors.joining()));
            hasWarned = true;
        }

        // We add the matched cameras to the known camera list
        for (var cam : notYetLoadedCams) {
            if (this.knownUsbCameras.stream().noneMatch(it -> usbCamEquals(it, cam))) {
                this.knownUsbCameras.add(cam);
            }
        }
        if (matchedCameras.isEmpty()) return null;

        // for unit tests only!
        if (!createSources) {
            return List.of();
        }

        // Turn these camera configs into vision sources
        var sources = loadVisionSourcesFromCamConfigs(matchedCameras);

        // Print info about each vision source
        for (var src : sources) {
            logger.debug(
                    () ->
                            "Matched config for camera \""
                                    + src.getFrameProvider().getName()
                                    + "\" and loaded "
                                    + src.getCameraConfiguration().pipelineSettings.size()
                                    + " pipelines");
        }

        return sources;
    }

    /**
     * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on
     * disk.
     *
     * @param detectedCamInfos Information about currently connected USB cameras.
     * @param loadedUsbCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
     * @return the matched configurations.
     */
    private List<CameraConfiguration> matchUSBCameras(
            List<UsbCameraInfo> detectedCamInfos, List<CameraConfiguration> loadedUsbCamConfigs) {
        var detectedCameraList = new ArrayList<>(detectedCamInfos);
        ArrayList<CameraConfiguration> cameraConfigurations = new ArrayList<>();

        // loop over all the configs loaded from disk
        for (CameraConfiguration config : loadedUsbCamConfigs) {
            UsbCameraInfo cameraInfo;

            // attempt matching by path and basename
            logger.debug(
                    "Trying to find a match for loaded camera "
                            + config.baseName
                            + " with path "
                            + config.path);
            cameraInfo =
                    detectedCameraList.stream()
                            .filter(
                                    usbCameraInfo ->
                                            usbCameraInfo.path.equals(config.path)
                                                    && cameraNameToBaseName(usbCameraInfo.name).equals(config.baseName))
                            .findFirst()
                            .orElse(null);

            // if path based fails, attempt basename only match
            if (cameraInfo == null) {
                logger.debug("Failed to match by path and name, falling back to name-only match");
                cameraInfo =
                        detectedCameraList.stream()
                                .filter(
                                        usbCameraInfo ->
                                                cameraNameToBaseName(usbCameraInfo.name).equals(config.baseName))
                                .findFirst()
                                .orElse(null);
            }

            // If we actually matched a camera to a config, remove that camera from the list and add it to
            // the output
            if (cameraInfo != null) {
                logger.debug("Matched the config for " + config.baseName + " to a physical camera!");
                detectedCameraList.remove(cameraInfo);
                cameraConfigurations.add(mergeInfoIntoConfig(config, cameraInfo));
            }
        }

        // If we have any unmatched cameras left, create a new CameraConfiguration for them here.
        logger.debug(
                "After matching loaded configs " + detectedCameraList.size() + " cameras were unmatched.");
        for (UsbCameraInfo info : detectedCameraList) {
            // create new camera config for all new cameras
            String baseName = cameraNameToBaseName(info.name);
            String uniqueName = baseNameToUniqueName(baseName);

            int suffix = 0;
            while (containsName(cameraConfigurations, uniqueName)) {
                suffix++;
                uniqueName = String.format("%s (%d)", uniqueName, suffix);
            }

            logger.info("Creating a new camera config for camera " + uniqueName);

            // HACK -- for picams, we want to use the camera model
            String nickname = uniqueName;
            if (isCsiCamera(info)) {
                nickname = LibCameraJNI.getSensorModel().toString();
            }

            CameraConfiguration configuration =
                    new CameraConfiguration(baseName, uniqueName, nickname, info.path, info.otherPaths);
            cameraConfigurations.add(configuration);
        }

        logger.debug("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    private boolean isCsiCamera(UsbCameraInfo configuration) {
        return (Arrays.stream(configuration.otherPaths).anyMatch(it -> it.contains("csi-video"))
                || cameraNameToBaseName(configuration.name).equals("unicam"));
    }

    private CameraConfiguration mergeInfoIntoConfig(CameraConfiguration cfg, UsbCameraInfo info) {
        if (!cfg.path.equals(info.path)) {
            logger.debug("Updating path config from " + cfg.path + " to " + info.path);
            cfg.path = info.path;
        }

        return cfg;
    }

    private List<UsbCameraInfo> filterAllowedDevices(List<UsbCameraInfo> allDevices) {
        List<UsbCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            if (deviceBlacklist.contains(device.name)) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name + "\" at \"" + device.path + "\"");
            } else {
                filteredDevices.add(device);
                logger.trace(
                        "Adding local video device - \"" + device.name + "\" at \"" + device.path + "\"");
            }
        }
        return filteredDevices;
    }

    private boolean usbCamEquals(UsbCameraInfo a, UsbCameraInfo b) {
        return a.path.equals(b.path)
                && a.dev == b.dev
                && a.name.equals(b.name)
                && a.productId == b.productId
                && a.vendorId == b.vendorId;
    }

    // Remove all non-ASCII characters
    private static String cameraNameToBaseName(String cameraName) {
        return cameraName.replaceAll("[^\\x00-\\x7F]", "");
    }

    // Replace spaces with underscores
    private static String baseNameToUniqueName(String baseName) {
        return baseName.replaceAll(" ", "_");
    }

    private static List<VisionSource> loadVisionSourcesFromCamConfigs(
            List<CameraConfiguration> camConfigs) {
        var cameraSources = new ArrayList<VisionSource>();
        for (var configuration : camConfigs) {
            System.out.println("Creating VisionSource for " + configuration);

            // Picams should have csi-video in the path
            boolean is_picam =
                    (Arrays.stream(configuration.otherPaths).anyMatch(it -> it.contains("csi-video"))
                            || configuration.baseName.equals("unicam"));
            boolean is_pi = Platform.isRaspberryPi();
            if (is_picam && is_pi) {
                configuration.cameraType = CameraType.ZeroCopyPicam;
                var piCamSrc = new LibcameraGpuSource(configuration);
                cameraSources.add(piCamSrc);
            } else {
                var newCam = new USBCameraSource(configuration);
                if (!newCam.cameraQuirks.hasQuirk(CameraQuirk.CompletelyBroken)
                        && !newCam.getSettables().videoModes.isEmpty()) {
                    cameraSources.add(newCam);
                }
            }
        }
        return cameraSources;
    }

    /**
     * Check if a given config list contains the given unique name.
     *
     * @param configList A list of camera configs.
     * @param uniqueName The unique name.
     * @return If the list of configs contains the unique name.
     */
    private boolean containsName(
            final List<CameraConfiguration> configList, final String uniqueName) {
        return configList.stream()
                .anyMatch(configuration -> configuration.uniqueName.equals(uniqueName));
    }
}

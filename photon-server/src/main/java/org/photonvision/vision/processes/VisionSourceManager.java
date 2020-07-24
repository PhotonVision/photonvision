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

package org.photonvision.vision.processes;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;

public class VisionSourceManager {

    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);
    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    private final List<UsbCameraInfo> knownUsbCameras = new CopyOnWriteArrayList<>();
    private final List<CameraConfiguration> unmatchedLoadedConfigs = new CopyOnWriteArrayList<>();

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private VisionSourceManager() {
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

    boolean hasYote = false;

    private void tryMatchUSBCams() {
        // Detect cameras using CSCore
        List<UsbCameraInfo> connectedCameras =
                new ArrayList<>(filterAllowedDevices(Arrays.asList(UsbCamera.enumerateUsbCameras())));

        if (!hasYote) {
            connectedCameras.remove(0);
            hasYote = true;
        }
        // Remove all known devices
        var notYetLoadedCams = new ArrayList<UsbCameraInfo>();
        for (var connectedCam : connectedCameras) {
            if (this.knownUsbCameras.stream().noneMatch(it -> usbCamEquals(it, connectedCam))) {
                notYetLoadedCams.add(connectedCam);
            }
        }
        if(notYetLoadedCams.isEmpty()) {
            logger.warn(unmatchedLoadedConfigs.size() + " configs are unmatched, but there are no unmatched USB cameras.");
            logger.warn("Check that all cameras are connected, or that the path is correct?");
            return;
        }
        logger.trace("Matching " + notYetLoadedCams.size() + " new cameras!");

        // Debug prints
        for (var info : notYetLoadedCams) {
            logger.info("Adding local video device - \"" + info.name + "\" at \"" + info.path + "\"");
        }

        // Sort out just the USB cams
        var usbCamConfigs = new ArrayList<>();
        for (var config : unmatchedLoadedConfigs) {
            if (config.cameraType == CameraType.UsbCamera) usbCamConfigs.add(config);
        }
        if (usbCamConfigs.isEmpty()) return;
        logger.info("Trying to match " + usbCamConfigs.size() + " unmatched configs...");

        // Match camera configs to physical cameras
        var matchedCameras = matchUSBCameras(notYetLoadedCams, unmatchedLoadedConfigs);
        unmatchedLoadedConfigs.removeAll(matchedCameras);
        logger.trace(
                () ->
                        "After matching, "
                                + unmatchedLoadedConfigs.size()
                                + " configs remained unmatched. Is your camera disconnected?");

        // We add the matched cameras to the known camera list
        for (var cam : notYetLoadedCams) {
            if (this.knownUsbCameras.stream().noneMatch(it -> it.name.equals(cam.name)))
                this.knownUsbCameras.add(cam);
        }
        if (matchedCameras.isEmpty()) return;

        // Turn these camera configs into vision sources
        var sources = loadVisionSourcesFromCamConfigs(matchedCameras);

        // These sources can be turned into USB cameras, which can be added to the config manager
        var visionSourceMap = new HashMap<VisionSource, List<CVPipelineSettings>>();
        for (var src : sources) {
            var usbSrc = (USBCameraSource) src;
            visionSourceMap.put(usbSrc, usbSrc.configuration.pipelineSettings);
            logger.trace(
                    () ->
                            "Matched config for camera \""
                                    + src.getFrameProvider().getName()
                                    + "\" and loaded "
                                    + usbSrc.configuration.pipelineSettings.size()
                                    + " pipelines");
        }

        logger.info("Adding " + visionSourceMap.size() + " configs to VMM.");
        ConfigManager.getInstance().addCameraConfigurations(visionSourceMap);
        var addedSources = VisionModuleManager.getInstance().addSources(visionSourceMap);
        addedSources.forEach(VisionModule::start);
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

            // Load by path vs by index -- if the path is numeric we'll match by index
            if (StringUtils.isNumeric(config.path)) {
                // match by index
                var index = Integer.parseInt(config.path);
                logger.trace(
                        "Trying to find a match for loaded camera " + config.baseName + " with index " + index);
                cameraInfo =
                        detectedCameraList.stream()
                                .filter(usbCameraInfo -> usbCameraInfo.dev == index)
                                .findFirst()
                                .orElse(null);
            } else {
                // matching by path
                logger.trace(
                        "Trying to find a match for loaded camera "
                                + config.baseName
                                + " with path "
                                + config.path);
                cameraInfo =
                        detectedCameraList.stream()
                                .filter(usbCameraInfo -> usbCameraInfo.path.equals(config.path))
                                .findFirst()
                                .orElse(null);
            }

            // If we actually matched a camera to a config, remove that camera from the list and add it to
            // the output
            if (cameraInfo != null) {
                logger.trace("Matched the config for " + config.baseName + " to a physical camera!");
                detectedCameraList.remove(cameraInfo);
                cameraConfigurations.add(config);
            }
        }

        // If we have any unmatched cameras left, create a new CameraConfiguration for them here.
        logger.trace(
                "After matching loaded configs " + detectedCameraList.size() + " cameras were unmatched.");
        for (UsbCameraInfo info : detectedCameraList) {
            // create new camera config for all new cameras
            String baseName =
                    info.name.replaceAll("[^\\x00-\\x7F]", ""); // Remove all non-ASCII characters
            String uniqueName = baseName.replaceAll(" ", "_"); // Replace spaces with underscores;

            int suffix = 0;
            while (containsName(cameraConfigurations, uniqueName)) {
                suffix++;
                uniqueName = String.format("%s (%d)", uniqueName, suffix);
            }

            logger.info("Creating a new camera config for camera " + uniqueName);

            CameraConfiguration configuration =
                    new CameraConfiguration(baseName, uniqueName, uniqueName, info.path);
            cameraConfigurations.add(configuration);
        }

        logger.trace("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    private List<VisionSource> loadVisionSourcesFromCamConfigs(List<CameraConfiguration> camConfigs) {
        List<VisionSource> usbCameraSources = new ArrayList<>();
        camConfigs.forEach(configuration -> usbCameraSources.add(new USBCameraSource(configuration)));
        return usbCameraSources;
    }

    private static List<UsbCameraInfo> filterAllowedDevices(List<UsbCameraInfo> allDevices) {
        List<UsbCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            if (deviceBlacklist.contains(device.name)) {
                logger.info(
                        "Skipping blacklisted device: \"" + device.name + "\" at \"" + device.path + "\"");
            } else {
                filteredDevices.add(device);
                logger.info(
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

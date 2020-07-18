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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.provider.NetworkFrameProvider;
import org.photonvision.vision.pipeline.CVPipelineSettings;

public class VisionSourceManager {

    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);
    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    private static List<UsbCameraInfo> filterAllowedDevices(List<UsbCameraInfo> allDevices) {
        List<UsbCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            if (deviceBlacklist.contains(device.name)) {
                logger.info(
                        "Skipping blacklisted device: \"" + device.name + "\" at \"" + device.path + "\"");
            } else {
                allDevices.add(device);
                logger.info(
                        "Adding local video device - \"" + device.name + "\" at \"" + device.path + "\"");
            }
        }
        return filteredDevices;
    }

    private List<UsbCameraInfo> loadedCameraInfos = new ArrayList<>();
    private List<CameraConfiguration> unusedConfigList = List.of();

    private VisionSourceManager() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(
                () -> this.retryLoadUSBCameras(loadedCameraInfos, unusedConfigList),
                3L,
                3L,
                TimeUnit.SECONDS);
    }

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
    * Load vision sources based on currently connected hardware.
    *
    * @param loadedConfigs The {@link CameraConfiguration}s loaded from disk.
    */
    public List<VisionSource> loadAllSources(Collection<CameraConfiguration> loadedConfigs) {
        logger.trace(() -> "Loading all sources...");

        List<UsbCameraInfo> usbCamInfos = Arrays.asList(UsbCamera.enumerateUsbCameras());
        logger.trace(() -> "CSCore detected " + usbCamInfos.size() + " USB cameras!");

        for (var usbCamInfo : usbCamInfos) {
            logger.info(
                    "Adding local video device - \"" + usbCamInfo.name + "\" at \"" + usbCamInfo.path + "\"");
        }

        loadedCameraInfos = new ArrayList<>(usbCamInfos);

        return LoadAllSources(loadedConfigs, usbCamInfos);
    }

    /**
    * Load vision sources based on currently connected hardware and preexisting configs.
    *
    * @param loadedConfigs The configs loaded from disk.
    * @param detectedCamInfos The {@link UsbCameraInfo}s detected by {@link
    *     UsbCamera#enumerateUsbCameras()}.
    */
    public List<VisionSource> LoadAllSources(
            Collection<CameraConfiguration> loadedConfigs, List<UsbCameraInfo> detectedCamInfos) {
        var loadedUsbCamConfigs =
                loadedConfigs.stream()
                        .filter(configuration -> configuration.cameraType == CameraType.UsbCamera)
                        .collect(Collectors.toList());
        // var HttpCamerasConfiguration = camerasConfiguration.stream().filter(configuration ->
        // configuration.cameraType == CameraType.HttpCamera);
        var matchedCameras = matchUSBCameras(detectedCamInfos, loadedUsbCamConfigs);

        // turn the matched cameras into VisionSources
        return loadVisionSourcesFromCamConfigs(matchedCameras);
    }

    private NetworkFrameProvider loadHTTPCamera(CameraConfiguration config) {
        throw new NotImplementedException("");
    }

    /**
    * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on
    * disk.
    *
    * @param detectedCamInfos Information about currently connected USB cameras.
    * @param loadedUsbCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
    */
    private List<CameraConfiguration> matchUSBCameras(
            List<UsbCameraInfo> detectedCamInfos, List<CameraConfiguration> loadedUsbCamConfigs) {
        var detectedCameraList = new ArrayList<>(detectedCamInfos);
        var unmatchedCameraConfigs = new ArrayList<>(loadedUsbCamConfigs);
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
                unmatchedCameraConfigs.remove(config);
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

        if (!unmatchedCameraConfigs.isEmpty())
            logger.trace(
                    unmatchedCameraConfigs.size()
                            + " configs loaded from disk could not be matched to a physical camera. Retrying in background...");
        unusedConfigList = unmatchedCameraConfigs;

        logger.trace("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    private List<VisionSource> loadVisionSourcesFromCamConfigs(List<CameraConfiguration> camConfigs) {
        List<VisionSource> usbCameraSources = new ArrayList<>();
        camConfigs.forEach(configuration -> usbCameraSources.add(new USBCameraSource(configuration)));
        return usbCameraSources;
    }

    /**
    * Retry the connection to USB cameras
    *
    * @param loadedCameras The USBCameraInfos already loaded
    * @param unusedConfigs Unused CameraConfiguration objects
    */
    public void retryLoadUSBCameras(
            List<UsbCameraInfo> loadedCameras, List<CameraConfiguration> unusedConfigs) {
        List<UsbCameraInfo> connectedCameras = Arrays.asList(UsbCamera.enumerateUsbCameras());
        List<UsbCameraInfo> unconnectedCameras = new ArrayList<>();

        // If the connected camera isn't connected to any known USB cameras, add it to the unconnected
        // list
        for (var connectedCam : connectedCameras) {
            if (loadedCameras.stream().noneMatch(it -> usbCamEquals(it, connectedCam))) {
                unconnectedCameras.add(connectedCam);
            }
        }

        // If we have no new cameras, just return
        if (unconnectedCameras.isEmpty()) return;

        logger.debug(
                "Found "
                        + unconnectedCameras.size()
                        + " unconnected cameras! Matching to "
                        + unusedConfigs.size()
                        + " unmatched configs...");

        // Match the unconnected cameras and turn them into VisionSources
        var matchedCameras = matchUSBCameras(unconnectedCameras, unusedConfigs);
        var sources = loadVisionSourcesFromCamConfigs(matchedCameras);

        // Added loaded usb cameras to loadedCameraInfos
        for (var matchedCam : matchedCameras) {
            Optional<UsbCameraInfo> info =
                    connectedCameras.stream().filter(it -> it.name.equals(matchedCam.baseName)).findAny();
            info.ifPresent(usbCameraInfo -> loadedCameraInfos.add(usbCameraInfo));
        }

        for (var source : sources) {
            logger.debug(
                    "Automatically connecting to camera "
                            + source.getSettables().getConfiguration().nickname);
        }

        var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();
        for (var src : sources) {
            var usbSrc = (USBCameraSource) src;
            collectedSources.put(usbSrc, usbSrc.configuration.pipelineSettings);
            logger.trace(
                    () ->
                            "[CamRetry] Matched config for camera \""
                                    + src.getFrameProvider().getName()
                                    + "\" and loaded "
                                    + usbSrc.configuration.pipelineSettings.size()
                                    + " pipelines");
        }

        logger.info("Auto-adding " + collectedSources.size() + " new cameras to VMM.");
        var newlyAddedModules = VisionModuleManager.getInstance().addSources(collectedSources);
        for (VisionModule m : newlyAddedModules) {
            m.start();
        }

        ConfigManager.getInstance().addCameraConfigurations(collectedSources);
        ConfigManager.getInstance().save();
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

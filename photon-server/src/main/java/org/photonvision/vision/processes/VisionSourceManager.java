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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.USBCameraSource;

public class VisionSourceManager {

    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);
    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    /**
    * Load vision sources based on currently connected hardware.
    *
    * @param loadedConfigs The {@link CameraConfiguration}s loaded from disk.
    */
    public static List<VisionSource> loadAllSources(Collection<CameraConfiguration> loadedConfigs) {
        return loadAllSources(
                loadedConfigs, filterAllowedDevices(Arrays.asList(UsbCamera.enumerateUsbCameras())));
    }

    /**
    * Load vision sources based on given cameras and configs.
    *
    * @param loadedConfigs The configs loaded from disk.
    * @param detectedCamInfos The cameras to attempt connection to.
    */
    public static List<VisionSource> loadAllSources(
            Collection<CameraConfiguration> loadedConfigs, List<UsbCameraInfo> detectedCamInfos) {
        var loadedUsbCamConfigs =
                loadedConfigs.stream()
                        .filter(configuration -> configuration.cameraType == CameraType.UsbCamera)
                        .collect(Collectors.toList());
        var matchedCameras = matchUSBCameras(detectedCamInfos, loadedUsbCamConfigs);

        // turn the matched cameras into VisionSources
        return loadVisionSourcesFromCamConfigs(matchedCameras);
    }

    private static List<UsbCameraInfo> filterAllowedDevices(List<UsbCameraInfo> allDevices) {
        List<UsbCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            var deviceInfoStr =
                    "\""
                            + device.name
                            + "\" at \""
                            + device.path
                            + "\" with USB ID \""
                            + device.vendorId
                            + ":"
                            + device.productId
                            + "\"";
            if (deviceBlacklist.contains(device.name)) {
                logger.info("Skipping blacklisted device - " + deviceInfoStr);
            } else {
                filteredDevices.add(device);
                logger.info("Adding local video device - " + deviceInfoStr);
            }
        }
        return filteredDevices;
    }

    /**
    * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on
    * disk.
    *
    * @param detectedCamInfos Information about currently connected USB cameras.
    * @param loadedUsbCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
    */
    private static List<CameraConfiguration> matchUSBCameras(
            List<UsbCameraInfo> detectedCamInfos, List<CameraConfiguration> loadedUsbCamConfigs) {
        ArrayList<UsbCameraInfo> detectedCameraList = new ArrayList<>(detectedCamInfos);
        List<CameraConfiguration> cameraConfigurations = new ArrayList<>();

        // loop over all the configs loaded from disk
        for (CameraConfiguration config : loadedUsbCamConfigs) {
            UsbCameraInfo cameraInfo;

            // Load by path vs by index -- if the path is numeric we'll match by index
            if (StringUtils.isNumeric(config.path)) {
                // match by index
                var index = Integer.parseInt(config.path);
                logger.debug(
                        "Trying to find a match for loaded camera " + config.baseName + " with index " + index);
                cameraInfo =
                        detectedCameraList.stream()
                                .filter(usbCameraInfo -> usbCameraInfo.dev == index)
                                .findFirst()
                                .orElse(null);
            } else {
                // matching by path
                logger.debug(
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
                logger.debug("Matched the config for " + config.baseName + " to a physical camera!");
                detectedCameraList.remove(cameraInfo);
                cameraConfigurations.add(config);
            }
        }

        // If we have any unmatched cameras left, create a new CameraConfiguration for them here.
        logger.debug(
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

        logger.debug("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    private static List<VisionSource> loadVisionSourcesFromCamConfigs(
            List<CameraConfiguration> camConfigs) {
        List<VisionSource> usbCameraSources = new ArrayList<>();
        camConfigs.forEach(configuration -> usbCameraSources.add(new USBCameraSource(configuration)));
        return usbCameraSources;
    }

    /**
    * Check if a given config list contains the given unique name.
    *
    * @param configList A list of camera configs.
    * @param uniqueName The unique name.
    * @return If the list of configs contains the unique name.
    */
    private static boolean containsName(
            final List<CameraConfiguration> configList, final String uniqueName) {
        return configList.stream()
                .anyMatch(configuration -> configuration.uniqueName.equals(uniqueName));
    }
}

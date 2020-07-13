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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.provider.NetworkFrameProvider;

public class VisionSourceManager {

    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);

    /**
     * Load vision sources based on currently connected hardware.
     *
     * @param loadedConfigs The {@link CameraConfiguration}s loaded from disk.
     */
    public static List<VisionSource> loadAllSources(Collection<CameraConfiguration> loadedConfigs) {
        logger.trace(() -> "Loading all sources...");

        List<UsbCameraInfo> usbCamInfos = Arrays.asList(UsbCamera.enumerateUsbCameras());
        logger.trace(() -> "CSCore detected " + usbCamInfos.size() + " USB cameras!");

        for (var usbCamInfo : usbCamInfos) {
            logger.info("Adding local video device - \"" + usbCamInfo.name + "\" at \"" + usbCamInfo.path + "\"");
        }

        return LoadAllSources(loadedConfigs, usbCamInfos);
    }

    /**
     * Load vision sources based on currently connected hardware and preexisting configs.
     *
     * @param loadedConfigs    The configs loaded from disk.
     * @param detectedCamInfos The {@link UsbCameraInfo}s detected by {@link UsbCamera#enumerateUsbCameras()}.
     */
    public static List<VisionSource> LoadAllSources(
        Collection<CameraConfiguration> loadedConfigs, List<UsbCameraInfo> detectedCamInfos) {
        var loadedUsbCamConfigs =
            loadedConfigs.stream()
                .filter(configuration -> configuration.cameraType == CameraType.UsbCamera)
                .collect(Collectors.toList());
        // var HttpCamerasConfiguration = camerasConfiguration.stream().filter(configuration ->
        // configuration.cameraType == CameraType.HttpCamera);
        var matchedCameras = matchUSBCameras(detectedCamInfos, loadedUsbCamConfigs);
        return loadUSBCameraSources(matchedCameras);
    }

    private static NetworkFrameProvider loadHTTPCamera(CameraConfiguration config) {
        throw new NotImplementedException("");
    }

    /**
     * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on disk.
     *
     * @param detectedCamInfos    Information about currently connected USB cameras.
     * @param loadedUsbCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
     */
    private static List<CameraConfiguration> matchUSBCameras(List<UsbCameraInfo> detectedCamInfos,
                                                             List<CameraConfiguration> loadedUsbCamConfigs) {
        ArrayList<UsbCameraInfo> detectedCameraList = new ArrayList<>(detectedCamInfos);
        List<CameraConfiguration> cameraConfigurations = new ArrayList<>();

        // loop over all the configs loaded from disk
        for (CameraConfiguration config : loadedUsbCamConfigs) {
            UsbCameraInfo cameraInfo;

            // Load by path vs by index -- if the path is numeric we'll match by index
            if (StringUtils.isNumeric(config.path)) {
                // match by index
                cameraInfo =
                    detectedCameraList.stream()
                        .filter(usbCameraInfo -> usbCameraInfo.dev == Integer.parseInt(config.path))
                        .findFirst()
                        .orElse(null);
            } else {
                // matching by path
                cameraInfo =
                    detectedCameraList.stream()
                        .filter(usbCameraInfo -> usbCameraInfo.path.equals(config.path))
                        .findFirst()
                        .orElse(null);
            }

            // If we actually matched a camera to a config, remove that camera from the list and add it to
            // the output
            if (cameraInfo != null) {
                detectedCameraList.remove(cameraInfo);
                cameraConfigurations.add(config);
            }
        }

        // If we have any unmatched cameras left, create a new CameraConfiguration for them here.
        for (UsbCameraInfo info : detectedCameraList) {
            // create new camera config for all new cameras
            String baseName = info.name
                .replaceAll("[^\\x00-\\x7F]", ""); // Remove all non-ASCII characters
            String uniqueName = baseName.replaceAll(" ", "_"); // Replace spaces with underscores;

            int suffix = 0;
            while (containsName(cameraConfigurations, uniqueName)) {
                suffix++;
                uniqueName = String.format("%s (%d)", uniqueName, suffix);
            }

            CameraConfiguration configuration =
                new CameraConfiguration(baseName, uniqueName, uniqueName, info.path);
            cameraConfigurations.add(configuration);
        }

        return cameraConfigurations;
    }

    private static List<VisionSource> loadUSBCameraSources(List<CameraConfiguration> configurations) {
        List<VisionSource> usbCameraSources = new ArrayList<>();
        configurations.forEach(
            configuration -> usbCameraSources.add(new USBCameraSource(configuration)));
        return usbCameraSources;
    }

    /**
     * Check if a given config list contains the given unique name.
     *
     * @param configList A list of camera configs.
     * @param uniqueName The unique name.
     * @return If the list of configs contains the unique name.
     */
    private static boolean containsName(final List<CameraConfiguration> configList, final String uniqueName) {
        return configList.stream().anyMatch(configuration -> configuration.uniqueName.equals(uniqueName));
    }
}

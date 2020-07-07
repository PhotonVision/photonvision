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

    public static List<VisionSource> loadAllSources(
            Collection<CameraConfiguration> camerasConfiguration) {
        List<UsbCameraInfo> usbCamInfos = Arrays.asList(UsbCamera.enumerateUsbCameras());

        for (var usbCamInfo : usbCamInfos) {
            logger.info(
                    "Adding local video device - \"" + usbCamInfo.name + "\" at \"" + usbCamInfo.path + "\"");
        }

        return LoadAllSources(camerasConfiguration, usbCamInfos);
    }

    public static List<VisionSource> LoadAllSources(
            Collection<CameraConfiguration> camerasConfiguration, List<UsbCameraInfo> usbCameraInfos) {
        var UsbCamerasConfiguration =
                camerasConfiguration.stream()
                        .filter(configuration -> configuration.cameraType == CameraType.UsbCamera)
                        .collect(Collectors.toList());
        // var HttpCamerasConfiguration = camerasConfiguration.stream().filter(configuration ->
        // configuration.cameraType == CameraType.HttpCamera);
        var matchedCameras = matchUSBCameras(usbCameraInfos, UsbCamerasConfiguration);
        return loadUSBCameraSources(matchedCameras);
    }

    private static NetworkFrameProvider loadHTTPCamera(CameraConfiguration config) {
        throw new NotImplementedException("");
    }

    private static List<CameraConfiguration> matchUSBCameras(
            List<UsbCameraInfo> infos, List<CameraConfiguration> cameraConfigurationList) {
        ArrayList<UsbCameraInfo> loopableInfo = new ArrayList<>(infos);
        List<CameraConfiguration> cameraConfigurations = new ArrayList<>();

        for (CameraConfiguration config : cameraConfigurationList) {
            UsbCameraInfo cameraInfo;
            if (!StringUtils.isNumeric(config.path)) {
                // matching by path
                cameraInfo =
                        loopableInfo.stream()
                                .filter(usbCameraInfo -> usbCameraInfo.path.equals(config.path))
                                .findFirst()
                                .orElse(null);
            } else {
                // match by index
                cameraInfo =
                        loopableInfo.stream()
                                .filter(usbCameraInfo -> usbCameraInfo.dev == Integer.parseInt(config.path))
                                .findFirst()
                                .orElse(null);
            }

            if (cameraInfo != null) {
                loopableInfo.remove(cameraInfo);
                cameraConfigurations.add(config);
            }
        }
        for (UsbCameraInfo info : loopableInfo) {
            // create new camera config for all new cameras
            String name = info.name.replaceAll("[^\\x00-\\x7F]", "");
            String uniqueName = name;
            int suffix = 0;

            while (containsName(cameraConfigurations, uniqueName)) {
                suffix++;
                uniqueName = String.format("%s (%d)", uniqueName, suffix);
            }

            CameraConfiguration configuration =
                    new CameraConfiguration(name, uniqueName, uniqueName, info.path);
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

    private static boolean containsName(final List<CameraConfiguration> list, final String name) {
        return list.stream().anyMatch(configuration -> configuration.uniqueName.equals(name));
    }
}

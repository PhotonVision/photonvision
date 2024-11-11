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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.Platform.OSType;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.camera.CameraInfo;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.LibcameraGpuSource;
import org.photonvision.vision.camera.TestSource;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);

    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");
    private String ignoredCamerasRegex = "";

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    VisionSourceManager() {}

    void registerLoadedConfigs(CameraConfiguration... configs) {
        registerLoadedConfigs(Arrays.asList(configs));
    }

    /**
     * Register new camera configs loaded from disk. This will create vision modules for each camera
     * config and start them.
     *
     * @param configs The loaded camera configs.
     */
    public void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        logger.info("Registering loaded camera configs");
        var loadedConfigs = new CopyOnWriteArrayList<>(configs);
        var allDevices = new ArrayList<CameraInfo>();
        allDevices.addAll(getConnectedUSBCameras());
        allDevices.addAll(getConnectedCSICameras());
        allDevices = filterAllowedDevices(allDevices, Platform.getCurrentPlatform());

        for (var info : allDevices) {
            var matchingConfig = loadedConfigs.stream()
                            .filter(config -> config.path.equals(info.path))
                            .findFirst();
            if (matchingConfig.isPresent()) {
                var config = matchingConfig.get();
                logger.info("Found loaded config for camera " + config.uniqueName);
                config = mergeInfoIntoConfig(config, info);
            } else {
                var config = createConfigForCameras(info, loadedConfigs);
                config = mergeInfoIntoConfig(config, info);
                loadedConfigs.add(config);
            }
        }

        var modules = VisionModuleManager.getInstance().addSources(
                loadVisionSourcesFromCamConfigs(loadedConfigs, true));

        logger.info("Loaded " + loadedConfigs.size() + " camera configs");

        for (var module : modules) {
            logger.info("Starting vision module for camera " + module.visionSource.cameraConfiguration.uniqueName);
            module.start();
        }

        logger.info("Finished registering loaded camera configs");
    }

    public CameraConfiguration addNewVisionSource(CameraInfo info) {
        var config = createConfigForCameras(info, new ArrayList<>());
        VisionModuleManager.getInstance().addSources(
                loadVisionSourcesFromCamConfigs(List.of(config), true))
                .forEach(m -> m.start());
        return config;
    }

    /**
     * Pre filter out any csi cameras to return just USB Cameras. Allow defining the camerainfo.
     *
     * @return a list containing usbcamerainfo.
     */
    protected List<CameraInfo> getConnectedUSBCameras() {
        List<CameraInfo> cameraInfos =
                List.of(UsbCamera.enumerateUsbCameras()).stream()
                        .map(c -> new CameraInfo(c))
                        .collect(Collectors.toList());
        return cameraInfos;
    }

    /**
     * Retrieve the list of csi cameras from libcamera.
     *
     * @return a list containing csicamerainfo.
     */
    protected List<CameraInfo> getConnectedCSICameras() {
        List<CameraInfo> cameraInfos = new ArrayList<CameraInfo>();
        if (LibCameraJNILoader.isSupported())
            for (String path : LibCameraJNI.getCameraNames()) {
                String name = LibCameraJNI.getSensorModel(path).getFriendlyName();
                cameraInfos.add(
                        new CameraInfo(-1, path, name, new String[] {}, -1, -1, CameraType.ZeroCopyPicam));
            }
        return cameraInfos;
    }

    /**
     * Create new {@link CameraConfiguration}s for unmatched cameras, and assign them a unique name
     * (unique in the set of (loaded configs, unloaded configs, loaded vision modules) at least)
     */
    private CameraConfiguration createConfigForCameras(
            CameraInfo info,
            List<CameraConfiguration> loadedConfigs) {
        // create new camera config for all new cameras
        String baseName = info.getBaseName();
        String uniqueName = info.getHumanReadableName();

        int suffix = 0;
        while (containsName(loadedConfigs, uniqueName)
                || containsName(uniqueName)) {
            suffix++;
            uniqueName = String.format("%s (%d)", uniqueName, suffix);
        }

        logger.info("Creating a new camera config for camera " + uniqueName);

        String nickname = uniqueName;

        CameraConfiguration configuration =
                new CameraConfiguration(baseName, uniqueName, nickname, info.path, info.otherPaths);

        configuration.cameraType = info.cameraType;

        return configuration;
    }

    private CameraConfiguration mergeInfoIntoConfig(CameraConfiguration cfg, CameraInfo info) {
        if (!cfg.path.equals(info.path)) {
            logger.debug("Updating path config from " + cfg.path + " to " + info.path);
            cfg.path = info.path;
        }
        cfg.otherPaths = info.otherPaths;
        cfg.cameraType = info.cameraType;

        if (cfg.otherPaths.length != info.otherPaths.length) {
            logger.debug(
                    "Updating otherPath config from "
                            + Arrays.toString(cfg.otherPaths)
                            + " to "
                            + Arrays.toString(info.otherPaths));
            cfg.otherPaths = info.otherPaths.clone();
        } else {
            for (int i = 0; i < info.otherPaths.length; i++) {
                if (!cfg.otherPaths[i].equals(info.otherPaths[i])) {
                    logger.debug(
                            "Updating otherPath config from "
                                    + Arrays.toString(cfg.otherPaths)
                                    + " to "
                                    + Arrays.toString(info.otherPaths));
                    cfg.otherPaths = info.otherPaths.clone();
                    break;
                }
            }
        }

        return cfg;
    }

    public void setIgnoredCamerasRegex(String ignoredCamerasRegex) {
        this.ignoredCamerasRegex = ignoredCamerasRegex;
    }

    /**
     * Filter out any blacklisted or ignored devices.
     *
     * @param allDevices
     * @return list of devices with blacklisted or ignore devices removed.
     */
    private ArrayList<CameraInfo> filterAllowedDevices(List<CameraInfo> allDevices, Platform platform) {
        ArrayList<CameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            if (deviceBlacklist.contains(device.name)) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name + "\" at \"" + device.path + "\"");
            } else if (device.name.matches(ignoredCamerasRegex)) {
                logger.trace("Skipping ignored device: \"" + device.name + "\" at \"" + device.path);
            } else if (device.getIsV4lCsiCamera()) {
            } else if (device.otherPaths.length == 0
                    && platform.osType == OSType.LINUX
                    && device.cameraType == CameraType.UsbCamera) {
                logger.trace(
                        "Skipping device with no other paths: \"" + device.name + "\" at \"" + device.path);
                // If cscore hasnt passed this other paths aka a path by id or a path as in usb port then we
                // cant guarantee it is a valid camera.
            } else {
                filteredDevices.add(device);
                logger.trace(
                        "Adding local video device - \"" + device.name + "\" at \"" + device.path + "\"");
            }
        }
        return filteredDevices;
    }

    private static List<VisionSource> loadVisionSourcesFromCamConfigs(
            List<CameraConfiguration> camConfigs, boolean createSources) {
        var cameraSources = new ArrayList<VisionSource>();
        for (var configuration : camConfigs) {
            // In unit tests, create dummy
            if (!createSources) {
                cameraSources.add(new TestSource(configuration));
                continue;
            }

            boolean is_pi = Platform.isRaspberryPi();

            if (configuration.cameraType == CameraType.ZeroCopyPicam && is_pi) {
                // If the camera was loaded from libcamera then create its source using libcamera.
                var piCamSrc = new LibcameraGpuSource(configuration);
                cameraSources.add(piCamSrc);
            } else {
                var newCam = new USBCameraSource(configuration);
                if (!newCam.getCameraQuirks().hasQuirk(CameraQuirk.CompletelyBroken)
                        && !newCam.getSettables().videoModes.isEmpty()) {
                    cameraSources.add(newCam);
                }
            }
            logger.debug("Creating VisionSource for " + configuration.toShortString());
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

    /**
     * Check if the current list of known cameras contains the given unique name.
     *
     * @param uniqueName The unique name.
     * @return If the list of cameras contains the unique name.
     */
    private boolean containsName(final String uniqueName) {
        return VisionModuleManager.getInstance().getModules().stream()
                .anyMatch(camera -> camera.visionSource.cameraConfiguration.uniqueName.equals(uniqueName));
    }
}

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
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.camera.CameraInfo;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.LibcameraGpuSource;
import org.photonvision.vision.camera.TestSource;
import org.photonvision.vision.camera.USBCameraSource;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);
    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    final List<CameraInfo> knownCameras = new CopyOnWriteArrayList<>();

    final List<CameraConfiguration> unmatchedLoadedConfigs = new CopyOnWriteArrayList<>();
    private boolean hasWarned;
    private boolean hasWarnedNoCameras = false;
    private String ignoredCamerasRegex = "";

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    VisionSourceManager() {
    }

    public void registerTimedTask() {
        TimedTaskManager.getInstance().addTask("VisionSourceManager", this::tryMatchCams, 3000);
    }

    public void registerLoadedConfigs(CameraConfiguration... configs) {
        registerLoadedConfigs(Arrays.asList(configs));
    }

    /**
     * Register new camera configs loaded from disk. This will add them to the list
     * of configs to try
     * to match, and also automatically spawn new vision processes as necessary.
     *
     * @param configs The loaded camera configs.
     */
    public void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        unmatchedLoadedConfigs.addAll(configs);
    }

    /**
     * Pre filter out any csi cameras to return just USB Cameras. Allow defining the
     * camerainfo.
     *
     * @return a list containing usbcamerainfo.
     */
    protected List<CameraInfo> getConnectedUSBCameras() {
        List<CameraInfo> cameraInfos = List.of(UsbCamera.enumerateUsbCameras()).stream()
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

    protected void tryMatchCams() {
        var visionSourceList = tryMatchCamImpl();
        if (visionSourceList == null)
            return;

        logger.info("Adding " + visionSourceList.size() + " configs to VMM.");
        ConfigManager.getInstance().addCameraConfigurations(visionSourceList);
        var addedSources = VisionModuleManager.getInstance().addSources(visionSourceList);
        addedSources.forEach(VisionModule::start);
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));
    }

    protected List<VisionSource> tryMatchCamImpl() {
        return tryMatchCamImpl(null);
    }

    /**
     * @param cameraInfos Used to feed camera info for unit tests.
     * @return New VisionSources.
     */
    protected List<VisionSource> tryMatchCamImpl(ArrayList<CameraInfo> cameraInfos) {
        boolean createSources = true;
        List<CameraInfo> connectedCameras;
        if (cameraInfos == null) {
            // Detect USB cameras using CSCore
            connectedCameras = new ArrayList<>(filterAllowedDevices(getConnectedUSBCameras()));
            // Detect CSI cameras using libcamera
            connectedCameras.addAll(new ArrayList<>(filterAllowedDevices(getConnectedCSICameras())));
        } else {
            connectedCameras = new ArrayList<>(filterAllowedDevices(cameraInfos));
            createSources = false; // Dont create sources if we are using supplied camerainfo for unit tests.
        }

        // Return no new sources because there are no new sources
        if (connectedCameras.isEmpty()) {
            if (!hasWarnedNoCameras) {
                logger.warn(
                        "No cameras were detected! Check that all cameras are connected, and that the path is correct.");
                hasWarnedNoCameras = true;
            }
            return null;
        } else
            hasWarnedNoCameras = false;

        // Remove any known cameras.
        connectedCameras.removeIf(c -> knownCameras.contains(c));

        // All cameras are already loaded return no new sources.
        if (connectedCameras.isEmpty())
            return null;

        logger.debug("Matching " + connectedCameras.size() + " new cameras!");

        // Debug prints
        for (var info : connectedCameras) {
            logger.info("Detected unmatched physical camera: " + info.toString());
        }

        if (!unmatchedLoadedConfigs.isEmpty())
            logger.debug("Trying to match " + unmatchedLoadedConfigs.size() + " unmatched configs...");

        // Match camera configs to physical cameras
        List<CameraConfiguration> matchedCameras = matchCameras(connectedCameras, unmatchedLoadedConfigs);

        unmatchedLoadedConfigs.removeAll(matchedCameras);
        if (!unmatchedLoadedConfigs.isEmpty() && !hasWarned) {
            logger.warn(
                    () -> "After matching, "
                            + unmatchedLoadedConfigs.size()
                            + " configs remained unmatched. Is your camera disconnected?");
            logger.warn(
                    "Unloaded configs: "
                            + unmatchedLoadedConfigs.stream()
                                    .map(it -> it.nickname)
                                    .collect(Collectors.joining(", ")));
            hasWarned = true;
        }

        // We add the matched cameras to the known camera list
        this.knownCameras.addAll(connectedCameras);

        if (matchedCameras.isEmpty())
            return null;

        // Turn these camera configs into vision sources
        var sources = loadVisionSourcesFromCamConfigs(matchedCameras, createSources);

        // Print info about each vision source
        for (var src : sources) {
            logger.debug(
                    () -> "Matched config for camera \""
                            + src.getFrameProvider().getName()
                            + "\" and loaded "
                            + src.getCameraConfiguration().pipelineSettings.size()
                            + " pipelines");
        }

        return sources;
    }

    private static final String camCfgToString(CameraConfiguration c) {
        return new StringBuilder()
                .append("[baseName=")
                .append(c.baseName)
                .append(", uniqueName=")
                .append(c.uniqueName)
                .append(", otherPaths=")
                .append(Arrays.toString(c.otherPaths))
                .append(", vid=")
                .append(c.usbVID)
                .append(", pid=")
                .append(c.usbPID)
                .append("]")
                .toString();
    }

    /**
     * Create {@link CameraConfiguration}s based on a list of detected USB cameras
     * and the configs on
     * disk.
     *
     * @param detectedCamInfos Information about currently connected USB cameras.
     * @param loadedCamConfigs The USB {@link CameraConfiguration}s loaded from
     *                         disk.
     * @return the matched configurations.
     */
    public List<CameraConfiguration> matchCameras(
            List<CameraInfo> detectedCamInfos,
            List<CameraConfiguration> loadedCamConfigs) {

        var detectedCameraList = new ArrayList<>(detectedCamInfos);
        ArrayList<CameraConfiguration> cameraConfigurations = new ArrayList<CameraConfiguration>();
        ArrayList<CameraConfiguration> unloadedConfigs = new ArrayList<CameraConfiguration>(loadedCamConfigs);

        logger.info("Matching all cameras by port");

        for (CameraConfiguration config : loadedCamConfigs) {
            logger.debug(
                    String.format(
                            "Trying to find a match for loaded camera %s (%s) with camera config: %s",
                            config.baseName, config.uniqueName, camCfgToString(config)));

            var cameraInfo = detectedCameraList.stream().filter((CameraInfo physicalCamera) -> {
                var savedPath = config.getUSBPath();
                return (savedPath.isPresent() && physicalCamera.getUSBPath().equals(savedPath));
            }).findFirst().orElse(null);

            // If we actually matched a camera to a config, remove that camera from the list
            // and add it to the output
            if (cameraInfo != null) {
                logger.debug(
                        "Matched the config for "
                                + config.uniqueName
                                + " to the physical camera config above!");
                cameraConfigurations.add(mergeInfoIntoConfig(config, cameraInfo));
                detectedCameraList.remove(cameraInfo);
                unloadedConfigs.remove(config);
            } else {
                logger.debug("No camera found for the config " + config.uniqueName);
            }
        }

        if (detectedCameraList.size() > 0) {
            cameraConfigurations.addAll(
                    createConfigsForCameras(detectedCameraList, unloadedConfigs, cameraConfigurations));
        }

        logger.debug("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    /**
     * Create new {@link CameraConfiguration}s for unmatched cameras, and assign
     * them a unique name
     * (unique in the set of (loaded configs, unloaded configs, loaded vision
     * modules) at least)
     */
    private List<CameraConfiguration> createConfigsForCameras(
            List<CameraInfo> detectedCameraList,
            List<CameraConfiguration> unloadedCamConfigs,
            List<CameraConfiguration> loadedConfigs) {
        List<CameraConfiguration> ret = new ArrayList<CameraConfiguration>();
        logger.debug(
                "After matching loaded configs, these configs remained unmatched: "
                        + detectedCameraList.stream()
                                .map(n -> String.valueOf(n))
                                .collect(Collectors.joining("-", "{", "}")));
        for (CameraInfo info : detectedCameraList) {
            // create new camera config for all new cameras
            String baseName = info.getBaseName();
            String uniqueName = info.getHumanReadableName();

            int suffix = 0;
            while (containsName(loadedConfigs, uniqueName)
                    || containsName(uniqueName)
                    || containsName(unloadedCamConfigs, uniqueName)
                    || containsName(ret, uniqueName)) {
                suffix++;
                uniqueName = String.format("%s (%d)", uniqueName, suffix);
            }

            logger.info("Creating a new camera config for camera " + uniqueName);

            String nickname = uniqueName;

            CameraConfiguration configuration = new CameraConfiguration(baseName, uniqueName, nickname, info.path,
                    info.otherPaths);

            configuration.cameraType = info.cameraType;

            ret.add(configuration);
        }
        return ret;
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
     * @return list of devices with blacklisted or ingore devices removed.
     */
    private List<CameraInfo> filterAllowedDevices(List<CameraInfo> allDevices) {
        List<CameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            if (deviceBlacklist.contains(device.name)) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name + "\" at \"" + device.path + "\"");
            } else if (device.name.matches(ignoredCamerasRegex)) {
                logger.trace("Skipping ignored device: \"" + device.name + "\" at \"" + device.path);
            } else if (device.getIsV4lCsiCamera()) {
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
            logger.debug("Creating VisionSource for " + camCfgToString(configuration));

            // In unit tests, create dummy
            if (!createSources) {
                cameraSources.add(new TestSource(configuration));
                continue;
            }

            boolean is_pi = Platform.isRaspberryPi();

            if (configuration.cameraType == CameraType.ZeroCopyPicam && is_pi) {
                // If the camera was loaded from libcamera then create its source using
                // libcamera.
                var piCamSrc = new LibcameraGpuSource(configuration);
                cameraSources.add(piCamSrc);
            } else {
                var newCam = new USBCameraSource(configuration);
                if (!newCam.getCameraQuirks().hasQuirk(CameraQuirk.CompletelyBroken)
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

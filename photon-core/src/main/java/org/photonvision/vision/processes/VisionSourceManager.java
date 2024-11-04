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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.Platform.OSType;
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
import org.photonvision.vision.camera.USBCameras.USBCameraSource;

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

    VisionSourceManager() {}

    public void registerTimedTask() {
        TimedTaskManager.getInstance().addTask("VisionSourceManager", this::tryMatchCams, 3000);
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

    protected void tryMatchCams() {
        var visionSourceList = tryMatchCamImpl();
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

    protected List<VisionSource> tryMatchCamImpl() {
        return tryMatchCamImpl(null);
    }

    protected List<VisionSource> tryMatchCamImpl(ArrayList<CameraInfo> cameraInfos) {
        return tryMatchCamImpl(cameraInfos, Platform.getCurrentPlatform());
    }

    /**
     * @param cameraInfos Used to feed camera info for unit tests.
     * @return New VisionSources.
     */
    protected List<VisionSource> tryMatchCamImpl(
            ArrayList<CameraInfo> cameraInfos, Platform platform) {
        boolean createSources = true;
        List<CameraInfo> connectedCameras;
        if (cameraInfos == null) {
            // Detect USB cameras using CSCore
            connectedCameras = new ArrayList<>(filterAllowedDevices(getConnectedUSBCameras(), platform));
            // Detect CSI cameras using libcamera
            connectedCameras.addAll(
                    new ArrayList<>(filterAllowedDevices(getConnectedCSICameras(), platform)));
        } else {
            connectedCameras = new ArrayList<>(filterAllowedDevices(cameraInfos, platform));
            createSources =
                    false; // Dont create sources if we are using supplied camerainfo for unit tests.
        }

        // Return no new sources because there are no new sources
        if (connectedCameras.isEmpty()) {
            if (!hasWarnedNoCameras) {
                logger.warn(
                        "No cameras were detected! Check that all cameras are connected, and that the path is correct.");
                hasWarnedNoCameras = true;
            }
            return null;
        } else hasWarnedNoCameras = false;

        // Remove any known cameras.
        connectedCameras.removeIf(c -> knownCameras.contains(c));

        // All cameras are already loaded return no new sources.
        if (connectedCameras.isEmpty()) return null;

        logger.debug("Matching " + connectedCameras.size() + " new camera(s)!");

        // Debug prints
        for (var info : connectedCameras) {
            logger.info("Detected unmatched physical camera: " + info.toString());
        }

        if (!unmatchedLoadedConfigs.isEmpty())
            logger.debug("Trying to match " + unmatchedLoadedConfigs.size() + " unmatched config(s)...");

        // Match camera configs to physical cameras
        List<CameraConfiguration> matchedCameras =
                matchCameras(connectedCameras, unmatchedLoadedConfigs);

        unmatchedLoadedConfigs.removeAll(matchedCameras);
        if (!unmatchedLoadedConfigs.isEmpty() && !hasWarned) {
            logger.warn(
                    () ->
                            "After matching, "
                                    + unmatchedLoadedConfigs.size()
                                    + " config(s) remained unmatched. Is your camera disconnected?");
            logger.warn(
                    "Unloaded configs: "
                            + unmatchedLoadedConfigs.stream()
                                    .map(it -> it.nickname)
                                    .collect(Collectors.joining(", ")));
            hasWarned = true;
        }

        // We add the matched cameras to the known camera list
        this.knownCameras.addAll(connectedCameras);

        if (matchedCameras.isEmpty()) return null;

        // Turn these camera configs into vision sources
        var sources = loadVisionSourcesFromCamConfigs(matchedCameras, createSources);

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
     * Get a predicate for checking cameras against a saved config.
     *
     * @param savedConfig The saved camera configuration to match against
     * @param checkUSBPath If we should compare the USB port/bus IDs
     * @param checkVidPid If we should compare USB VID and PID
     * @param checkBaseName If we should compare {@link CameraInfo#getBaseName}
     * @param checkPath If we should check {@link CameraInfo::path} (eg /dev/videoN on Linux, or
     *     ?/usb#vid_05c8&pid_03df&mi_00#7&fa76035&0&0000#{e5323777-f976-4f5b-9b55-b94699c46e44}\global
     *     on Windows)
     */
    private final Predicate<CameraInfo> getCameraMatcher(
            final CameraConfiguration savedConfig,
            boolean checkUSBPath,
            boolean checkVidPid,
            boolean checkBaseName,
            boolean checkPath) {
        if (checkUSBPath && savedConfig.getUSBPath().isEmpty()) {
            logger.debug(
                    "WARN: Camera has empty USB path, but asked to match by name: "
                            + savedConfig.toShortString());
        }

        return (CameraInfo physicalCamera) -> {
            var matches = true;

            if (checkUSBPath) {
                var savedPath = savedConfig.getUSBPath();
                matches &= (savedPath.isPresent() && physicalCamera.getUSBPath().equals(savedPath));
            }
            if (checkBaseName) {
                matches &= physicalCamera.getBaseName().equals(savedConfig.baseName);
            }
            if (checkVidPid) {
                matches &=
                        (physicalCamera.vendorId == savedConfig.usbVID
                                && physicalCamera.productId == savedConfig.usbPID);
            }
            if (checkPath) {
                matches &= (physicalCamera.path.equals(savedConfig.path));
            }

            matches &= (physicalCamera.cameraType == savedConfig.cameraType);

            return matches;
        };
    }

    /**
     * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on
     * disk.
     *
     * @param detectedCamInfos Information about currently connected USB cameras.
     * @param loadedCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
     * @return the matched configurations.
     */
    public List<CameraConfiguration> matchCameras(
            List<CameraInfo> detectedCamInfos, List<CameraConfiguration> loadedCamConfigs) {
        return matchCameras(
                detectedCamInfos,
                loadedCamConfigs,
                ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath);
    }

    /**
     * Create {@link CameraConfiguration}s based on a list of detected USB cameras and the configs on
     * disk.
     *
     * @param detectedCamInfos Information about currently connected USB cameras.
     * @param loadedCamConfigs The USB {@link CameraConfiguration}s loaded from disk.
     * @param matchCamerasOnlyByPath If we should never try to match only by (base name, vid, pid)
     * @return the matched configurations.
     */
    public List<CameraConfiguration> matchCameras(
            List<CameraInfo> detectedCamInfos,
            List<CameraConfiguration> loadedCamConfigs,
            boolean matchCamerasOnlyByPath) {
        var detectedCameraList = new ArrayList<>(detectedCamInfos);
        ArrayList<CameraConfiguration> cameraConfigurations = new ArrayList<CameraConfiguration>();
        ArrayList<CameraConfiguration> unloadedConfigs =
                new ArrayList<CameraConfiguration>(loadedCamConfigs);

        logger.info("Matching CSI cameras by port & base name...");
        cameraConfigurations.addAll(
                matchCamerasByStrategy(
                        detectedCameraList,
                        unloadedConfigs,
                        new CameraMatchingOptions(false, false, true, true, CameraType.ZeroCopyPicam)));

        logger.info("Matching USB cameras by usb port & name & USB VID/PID...");
        cameraConfigurations.addAll(
                matchCamerasByStrategy(
                        detectedCameraList,
                        unloadedConfigs,
                        new CameraMatchingOptions(true, true, true, false, CameraType.UsbCamera)));

        // On windows, the v4l path is actually useful and tells us the port the camera is physically
        // connected to which is neat
        if (Platform.isWindows() && !matchCamerasOnlyByPath) {
            logger.info("Matching USB cameras by windows-path & USB VID/PID only...");
            cameraConfigurations.addAll(
                    matchCamerasByStrategy(
                            detectedCameraList,
                            unloadedConfigs,
                            new CameraMatchingOptions(false, true, true, true, CameraType.UsbCamera)));
        }

        logger.info("Matching USB cameras by usb port & USB VID/PID...");
        cameraConfigurations.addAll(
                matchCamerasByStrategy(
                        detectedCameraList,
                        unloadedConfigs,
                        new CameraMatchingOptions(true, true, false, false, CameraType.UsbCamera)));

        // Legacy migration -- VID/PID will be unset, so we have to try with our most relaxed strategy
        // at least once. We _should_ still have a valid USB path (assuming cameras have not moved), so
        // try that first, then fallback to base name only beloow
        logger.info("Matching USB cameras by base-name & usb port...");
        cameraConfigurations.addAll(
                matchCamerasByStrategy(
                        detectedCameraList,
                        unloadedConfigs,
                        new CameraMatchingOptions(true, false, true, false, CameraType.UsbCamera)));

        // handle disabling only-by-base-name matching
        if (!matchCamerasOnlyByPath) {
            logger.info("Matching USB cameras by base-name & USB VID/PID only...");
            cameraConfigurations.addAll(
                    matchCamerasByStrategy(
                            detectedCameraList,
                            unloadedConfigs,
                            new CameraMatchingOptions(false, true, true, false, CameraType.UsbCamera)));

            // Legacy migration for if no USB VID/PID set
            logger.info("Matching USB cameras by base-name only...");
            cameraConfigurations.addAll(
                    matchCamerasByStrategy(
                            detectedCameraList,
                            unloadedConfigs,
                            new CameraMatchingOptions(false, false, true, false, CameraType.UsbCamera)));
        } else logger.info("Skipping match by filepath/vid/pid, disabled by user");

        if (detectedCameraList.size() > 0) {
            // handle disabling only-by-base-name matching
            if (!matchCamerasOnlyByPath) {
                cameraConfigurations.addAll(
                        createConfigsForCameras(detectedCameraList, unloadedConfigs, cameraConfigurations));
            } else {
                logger.warn(
                        "Not creating 'new' Photon CameraConfigurations for ["
                                + detectedCamInfos.stream()
                                        .map(CameraInfo::toString)
                                        .collect(Collectors.joining(";"))
                                + "], disabled by user");
            }
        }

        logger.debug("Matched or created " + cameraConfigurations.size() + " camera configs!");
        return cameraConfigurations;
    }

    /**
     * Abstractly match cameras
     *
     * @param detectedCamInfos Physical cameras unmatched and attached to the device
     * @param unloadedConfigs {@link CameraConfiguration}
     * @param checkUSBPath If we should compare the USB port/bus IDs
     * @param checkVidPid If we should compare USB VID and PID
     * @param checkBaseName If we should check {@link CameraInfo::getBaseName}
     * @param checkPath If we should check {@link CameraInfo::path} (eg /dev/videoN on Linux, or
     *     usb#vid_05c8&pid_03df&mi_00#7&fa76035&0&0000#{e5323777-f976-4f5b-9b55-b94699c46e44}\global
     *     on Windows). Note that path may change based on order cameras are plugged in/unplugged on
     *     Linux, and should not be trusted to remain the same.
     * @return All matched or created new configs
     */
    private List<CameraConfiguration> matchCamerasByStrategy(
            List<CameraInfo> detectedCamInfos,
            List<CameraConfiguration> unloadedConfigs,
            CameraMatchingOptions matchingOptions) {
        List<CameraConfiguration> ret = new ArrayList<CameraConfiguration>();
        List<CameraConfiguration> unloadedConfigsCopy =
                new ArrayList<CameraConfiguration>(unloadedConfigs);

        if (unloadedConfigsCopy.isEmpty()) return List.of();

        logger.debug("Matching with options " + matchingOptions.toString());

        for (CameraConfiguration config : unloadedConfigsCopy) {
            // Only run match path by id if the camera type is allowed. This allows us to specify matching
            // behavior per-camera-type
            if (matchingOptions.allowedTypes.contains(config.cameraType)) {
                logger.debug(
                        String.format(
                                "Trying to find a match for loaded camera %s (%s) with camera config: %s",
                                config.baseName, config.uniqueName, config.toShortString()));

                // Get matcher and filter against it, picking out the first match
                Predicate<CameraInfo> matches =
                        getCameraMatcher(
                                config,
                                matchingOptions.checkUSBPath,
                                matchingOptions.checkVidPid,
                                matchingOptions.checkBaseName,
                                matchingOptions.checkPath);
                var cameraInfo = detectedCamInfos.stream().filter(matches).findFirst().orElse(null);

                // If we actually matched a camera to a config, remove that camera from the list
                // and add it to the output
                if (cameraInfo != null) {
                    logger.debug(
                            "Matched the config for "
                                    + config.uniqueName
                                    + " to the physical camera config above!");
                    ret.add(mergeInfoIntoConfig(config, cameraInfo));
                    detectedCamInfos.remove(cameraInfo);
                    unloadedConfigs.remove(config);
                } else {
                    logger.debug("No camera found for the config " + config.uniqueName);
                }
            }
        }
        return ret;
    }

    /**
     * Create new {@link CameraConfiguration}s for unmatched cameras, and assign them a unique name
     * (unique in the set of (loaded configs, unloaded configs, loaded vision modules) at least)
     */
    private List<CameraConfiguration> createConfigsForCameras(
            List<CameraInfo> detectedCameraList,
            List<CameraConfiguration> unloadedCamConfigs,
            List<CameraConfiguration> loadedConfigs) {
        List<CameraConfiguration> ret = new ArrayList<CameraConfiguration>();
        logger.debug(
                "After matching loaded configs, these cameras remained unmatched: "
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

            CameraConfiguration configuration =
                    new CameraConfiguration(baseName, uniqueName, nickname, info.path, info.otherPaths);

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
     * @return list of devices with blacklisted or ignore devices removed.
     */
    private List<CameraInfo> filterAllowedDevices(List<CameraInfo> allDevices, Platform platform) {
        List<CameraInfo> filteredDevices = new ArrayList<>();
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

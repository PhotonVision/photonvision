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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.PhotonConfiguration.UICameraConfiguration;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.Platform.OSType;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.FileVisionSource;
import org.photonvision.vision.camera.LibcameraGpuSource;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.camera.UniqueCameraSummary;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);

    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Jackson does use these members even if your IDE claims otherwise
    public static class VisionSourceManagerState {
        public List<UniqueCameraSummary> activeCameras;
        public List<UICameraConfiguration> disabledCameras;
        public List<UniqueCameraSummary> allConnectedCameras;
    }

    // Map of (unique name) -> (all CameraConfigurations) that have been registered
    private final HashMap<String, CameraConfiguration> disabledCameraConfigs = new HashMap<>();

    // The subset of cameras that are "active", converted to VisionModules
    public VisionModuleManager vmm = new VisionModuleManager();

    public void registerTimedTasks() {
        TimedTaskManager.getInstance().addTask("CameraDeviceExplorer", this::discoverNewDevices, 1000);
    }

    /**
     * Register new camera configs loaded from disk. This will create vision modules for each camera
     * config and start them.
     *
     * @param configs The loaded camera configs.
     */
    public synchronized void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        logger.info("Registering loaded camera configs");

        final HashMap<String, CameraConfiguration> deserializedConfigs = new HashMap<>();

        // 1. Verify all camera unique names are unique and paths/types are unique for paranoia. This
        // seems redundant, consider deleting
        for (var config : configs) {
            Predicate<PVCameraInfo> checkDuplicateCamera =
                    (other) ->
                            (other.type().equals(config.matchedCameraInfo.type())
                                    && other.uniquePath().equals(config.matchedCameraInfo.uniquePath()));

            if (deserializedConfigs.containsKey(config.uniqueName)) {
                logger.error(
                        "Duplicate unique name for config " + config.uniqueName + " -- not overwriting");
            } else if (deserializedConfigs.values().stream()
                    .map(it -> it.matchedCameraInfo)
                    .anyMatch(checkDuplicateCamera)) {
                logger.error(
                        "Duplicate camera type & path for config " + config.uniqueName + " -- not overwriting");
            } else {
                deserializedConfigs.put(config.uniqueName, config);
            }
        }

        // 2. create sources -> VMMs for all active cameras and add to our VMM. We don't care about if
        // the underlying device is currently connected or not.
        deserializedConfigs.values().stream()
                .filter(it -> !it.deactivated)
                .map(this::loadVisionSourceFromCamConfig)
                .map(vmm::addSource)
                .forEach(VisionModule::start);

        // 3. write down all disabled sources for later
        deserializedConfigs.entrySet().stream()
                .filter(it -> it.getValue().deactivated)
                .forEach(it -> this.disabledCameraConfigs.put(it.getKey(), it.getValue()));

        logger.info(
                "Finished registering loaded camera configs! Started "
                        + vmm.getModules().size()
                        + " active VisionModules, with "
                        + deserializedConfigs.size()
                        + " disabled VisionModules");
    }

    /**
     * Reactivate a previously created vision source
     *
     * @param uniqueName
     */
    public synchronized boolean reactivateDisabledCameraConfig(String uniqueName) {
        // Make sure we have an old, currently -inactive- camera around
        var deactivatedConfig = Optional.ofNullable(this.disabledCameraConfigs.get(uniqueName));
        if (deactivatedConfig.isEmpty() || !deactivatedConfig.get().deactivated) {
            return false;
        }

        // transform the camera info all the way to a VisionModule and then start it
        var created =
                deactivatedConfig
                        .map(this::loadVisionSourceFromCamConfig)
                        .map(vmm::addSource)
                        .map(
                                it -> {
                                    it.start();
                                    it.saveAndBroadcastAll();
                                    return it;
                                })
                        .isPresent();

        // We have a new camera! Tell the world about it
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));

        pushUiUpdate();

        return created;
    }

    /**
     * Assign a camera that currently has no associated CameraConfiguration loaded. TODO: determine
     * how we should be addressing unmatched cameras - it seems quirky to assign a unique name to
     * PVCameraInfos, we should instead have the backend hand us sufficient data to reconstruct the
     * PVCameraInfo ourselves.
     *
     * @param uniqueName
     */
    public synchronized boolean assignUnmatchedCamera(String uniqueName) {
        // Check if the camera is already in use by another module
        final Predicate<PVCameraInfo> isNotUsedInModule =
                info ->
                        vmm.getModules().stream().noneMatch(module -> module.uniqueName().equals(uniqueName));
        // transform the camera info all the way to a VisionModule and then start it
        var created =
                Optional.ofNullable(cameraDeviceMap.get(uniqueName))
                        .filter(isNotUsedInModule)
                        // Make sure we aren't going to overwrite
                        .map(
                                info ->
                                        configFromUniqueName(uniqueName)
                                                .orElse(createConfigForCameras(info, uniqueName)))
                        .map(this::loadVisionSourceFromCamConfig)
                        .map(vmm::addSource)
                        .map(
                                it -> {
                                    it.start();
                                    it.saveAndBroadcastAll();
                                    return it;
                                })
                        .isPresent();

        // We have a new camera! Tell the world about it
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));

        pushUiUpdate();

        return created;
    }

    public synchronized boolean deactivateVisionSource(String uniqueName) {
        // try to find the module. If we find it, remove it from the VMM
        var removedConfig =
                vmm.getModules().stream()
                        .filter(module -> module.uniqueName().equals(uniqueName))
                        .findFirst()
                        .map(
                                it -> {
                                    vmm.removeModule(it);
                                    return it.getCameraConfiguration();
                                });

        if (removedConfig.isEmpty()) {
            logger.error("Could not find module " + uniqueName);
            return false;
        }

        // And stuff it into our list of disabled camera configs
        disabledCameraConfigs.put(removedConfig.get().uniqueName, removedConfig.get());

        pushUiUpdate();

        return true;
    }

    protected synchronized VisionSourceManagerState getVsmState() {
        var ret = new VisionSourceManagerState();

        ret.activeCameras =
                this.deserializedConfigs.values().stream()
                        .filter(it -> !it.deactivated)
                        .map(it -> new UniqueCameraSummary(it.uniqueName, cameraDeviceMap.get(it.uniqueName)))
                        .toList();
        ret.disabledCameras =
                this.deserializedConfigs.values().stream()
                        .filter(it -> it.deactivated)
                        .map(CameraConfiguration::toUiConfig)
                        .toList();

        // transform the camera info all the way to a VisionModule and then start it
        ret.allConnectedCameras =
                cameraDeviceMap.entrySet().stream()
                        .map(item -> new UniqueCameraSummary(item.getKey(), item.getValue()))
                        .toList();

        return ret;
    }

    /**
     * TODO: determine how we should be addressing unmatched cameras - it seems quirky to assign a
     * unique name to PVCameraInfos, we should instead have the backend hand us sufficient data to
     * reconstruct the PVCameraInfo ourselves.
     */
    protected synchronized void discoverNewDevices() {
        // Get all connected cameras
        List<PVCameraInfo> seenInfos = filterAllowedDevices(getConnectedCameras());
        Collection<PVCameraInfo> knownInfos = cameraDeviceMap.values();

        logger.info(cameraDeviceMap.keySet().toString());

        // Find all infos that have been seen but weren't known before
        seenInfos.stream()
                .filter(d -> !knownInfos.contains(d))
                .forEach(d -> cameraDeviceMap.put(uniqueName(d.name(), cameraDeviceMap.keySet()), d));

        knownInfos.removeIf(d -> !seenInfos.contains(d));

        // this is only ran every 5 seconds so we can afford to publish every time
        pushUiUpdate();
    }

    protected void pushUiUpdate() {
        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("discoveredCameras", getVsmState()));
    }

    protected List<PVCameraInfo> getConnectedCameras() {
        List<PVCameraInfo> cameraInfos = new ArrayList<>();
        // find all connected cameras
        // cscore can return usb and csi cameras but csi are filtered out
        Stream.of(UsbCamera.enumerateUsbCameras())
                .map(c -> PVCameraInfo.fromUsbCameraInfo(c))
                .filter(c -> !(String.join("", c.otherPaths()).contains("csi-video")))
                .filter(c -> !c.name().equals("unicam"))
                .forEach(cameraInfos::add);
        if (LibCameraJNILoader.isSupported()) {
            // find all CSI cameras (Raspberry Pi cameras)
            Stream.of(LibCameraJNI.getCameraNames())
                    .map(
                            path -> {
                                String name = LibCameraJNI.getSensorModel(path).getFriendlyName();
                                return PVCameraInfo.fromCSICameraInfo(path, name);
                            })
                    .forEach(cameraInfos::add);
        }
        return cameraInfos;
    }

    private static String uniqueName(String name, Collection<String> takenNames) {
        if (!takenNames.contains(name)) {
            return name;
        }
        int i = 1;
        while (takenNames.contains(name + i)) {
            i++;
        }
        return String.format("%s (%d)", name, i);
    }

    private static CameraConfiguration createConfigForCameras(PVCameraInfo info, String uniqueName) {
        // create new camera config for all new cameras
        logger.info("Creating a new camera config for camera " + uniqueName);

        CameraConfiguration configuration = new CameraConfiguration(info);

        return configuration;
    }

    private Optional<CameraConfiguration> configFromUniqueName(String uniqueName) {
        return Optional.ofNullable(this.deserializedConfigs.get(uniqueName));
    }

    private static List<PVCameraInfo> filterAllowedDevices(List<PVCameraInfo> allDevices) {
        Platform platform = Platform.getCurrentPlatform();
        ArrayList<PVCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            boolean valid = false;
            if (deviceBlacklist.contains(device.name())) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name() + "\" at \"" + device.path() + "\"");
            } else if (device instanceof PVCameraInfo.PVUsbCameraInfo usbDevice) {
                if (usbDevice.otherPaths.length == 0
                        && platform.osType == OSType.LINUX
                        && device.type() == CameraType.UsbCamera) {
                    logger.trace(
                            "Skipping device with no other paths: \""
                                    + device.name()
                                    + "\" at \""
                                    + device.path());
                } else if (Arrays.stream(usbDevice.otherPaths).anyMatch(it -> it.contains("csi-video"))
                        || usbDevice.name().equals("unicam")) {
                    logger.trace(
                            "Skipping CSI device from CSCore: \""
                                    + device.name()
                                    + "\" at \""
                                    + device.path()
                                    + "\"");
                } else {
                    valid = true;
                }
            } else {
                valid = true;
            }
            if (valid) {
                filteredDevices.add(device);
                logger.trace(
                        "Adding local video device - \"" + device.name() + "\" at \"" + device.path() + "\"");
            }
        }
        return filteredDevices;
    }

    /**
     * Convert a configuration into a VisionSource. The VisionSource type is pulled from the {@link
     * CameraConfiguration}'s matchedCameraInfo. We depend on the underlying {@link VisionSource} to
     * be robust to disconnected sources at boot
     */
    protected VisionSource loadVisionSourceFromCamConfig(CameraConfiguration configuration) {
        VisionSource source =
                switch (configuration.matchedCameraInfo.type()) {
                    case UsbCamera -> new USBCameraSource(configuration);
                    case ZeroCopyPicam -> new LibcameraGpuSource(configuration);
                    case FileCamera -> new FileVisionSource(configuration);
                };

        logger.debug("Creating VisionSource for " + configuration.toShortString());
        return source;
    }

    public List<VisionModule> getVisionModules() {
        return vmm.getModules();
    }
}

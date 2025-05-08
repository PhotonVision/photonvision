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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.websocket.UICameraConfiguration;
import org.photonvision.common.dataflow.websocket.UIPhotonConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.Platform.OSType;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.FileVisionSource;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.camera.csi.LibcameraGpuSource;

/**
 * This class manages starting up VisionModules for serialized devices ({@link
 * VisionSourceManager#loadVisionSourceFromCamConfig}), as well as handling requests from users to
 * disable (release the camera device, but keep the configuration around) ({@link
 * VisionSourceManager#deactivateVisionSource}), reactivate (recreate a VisionModule from a saved
 * and currently disabled configuration) ({@link
 * VisionSourceManager#reactivateDisabledCameraConfig}), and create a new VisionModule from a {@link
 * PVCameraInfo} ({@link VisionSourceManager#assignUnmatchedCamera}).
 *
 * <p>We now require user interaction for pretty much every operation this undertakes.
 */
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
        public List<UICameraConfiguration> disabledConfigs;
        public List<PVCameraInfo> allConnectedCameras;
    }

    // Map of (unique name) -> (all CameraConfigurations) that have been registered
    protected final HashMap<String, CameraConfiguration> disabledCameraConfigs = new HashMap<>();

    // The subset of cameras that are "active", converted to VisionModules
    public VisionModuleManager vmm = new VisionModuleManager();

    public void registerTimedTasks() {
        TimedTaskManager.getInstance().addTask("CameraDeviceExplorer", this::pushUiUpdate, 1000);
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
        var deactivatedConfig = Optional.ofNullable(this.disabledCameraConfigs.remove(uniqueName));
        if (deactivatedConfig.isEmpty() || !deactivatedConfig.get().deactivated) {
            // Not in map, give up
            return false;
        }

        // Check if the camera is already in use by another module
        if (vmm.getModules().stream()
                .anyMatch(
                        module ->
                                module
                                        .getCameraConfiguration()
                                        .matchedCameraInfo
                                        .uniquePath()
                                        .equals(deactivatedConfig.get().matchedCameraInfo.uniquePath()))) {
            logger.error(
                    "Camera unique-path already in use by active VisionModule! Cannot reactivate "
                            + deactivatedConfig.get().nickname);
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

        if (!created) {
            // Couldn't create a VM for this config - restore state
            this.disabledCameraConfigs.put(uniqueName, deactivatedConfig.get());
        }

        // We have a new camera! Tell the world about it
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));

        pushUiUpdate();

        return created;
    }

    /**
     * Assign a camera that currently has no associated CameraConfiguration loaded.
     *
     * @param cameraInfo
     */
    public synchronized boolean assignUnmatchedCamera(PVCameraInfo cameraInfo) {
        // Check if the camera is already in use by another module
        if (vmm.getModules().stream()
                .anyMatch(
                        module ->
                                module
                                        .getCameraConfiguration()
                                        .matchedCameraInfo
                                        .uniquePath()
                                        .equals(cameraInfo.uniquePath()))) {
            logger.error(
                    "Camera unique-path already in use by active VisionModule! Cannot add " + cameraInfo);
            return false;
        }

        var source = loadVisionSourceFromCamConfig(new CameraConfiguration(cameraInfo));
        var module = vmm.addSource(source);

        module.start();

        // We have a new camera! Tell the world about it
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));

        pushUiUpdate();

        return true;
    }

    public synchronized boolean deleteVisionSource(String uniqueName) {
        deactivateVisionSource(uniqueName);
        var config = disabledCameraConfigs.remove(uniqueName);
        ConfigManager.getInstance().getConfig().removeCameraConfig(uniqueName);
        ConfigManager.getInstance().saveToDisk();

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));
        pushUiUpdate();

        return config != null;
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

        logger.info("Disabled the VisionModule for " + removedConfig.get().nickname);

        pushUiUpdate();

        return true;
    }

    protected synchronized VisionSourceManagerState getVsmState() {
        var ret = new VisionSourceManagerState();

        ret.allConnectedCameras = filterAllowedDevices(getConnectedCameras());
        ret.disabledConfigs =
                disabledCameraConfigs.values().stream().map(it -> it.toUiConfig()).toList();

        return ret;
    }

    protected void pushUiUpdate() {
        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("visionSourceManager", getVsmState()));
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

        // FileVisionSources are a bit quirky. They aren't enumerated by the above, but i still want my
        // UI to look like it ought to work
        vmm.getModules().stream()
                .map(it -> it.getCameraConfiguration().matchedCameraInfo)
                .filter(info -> info instanceof PVCameraInfo.PVFileCameraInfo)
                .forEach(cameraInfos::add);

        return cameraInfos;
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
     *
     * <p>Verify that nickname is unique within the set of deserialized camera configurations, adding
     * random characters if this isn't the case
     */
    protected VisionSource loadVisionSourceFromCamConfig(CameraConfiguration configuration) {
        logger.debug("Creating VisionSource for " + configuration.toShortString());

        // First, make sure that nickname is globally unique since we use the nickname in NetworkTables.
        // "Just one more source of truth bro it'll real this time I promise"
        var currentNicknames = new ArrayList<String>();
        this.disabledCameraConfigs.values().stream()
                .map(it -> it.nickname)
                .forEach(currentNicknames::add);
        this.vmm.getModules().stream()
                .map(it -> it.getCameraConfiguration().nickname)
                .forEach(currentNicknames::add);
        // while it's a duplicate
        while (currentNicknames.contains(configuration.nickname)) {
            // if we already have a number, extract
            var pattern = Pattern.compile("(^.*) \\(([0-9]+)\\)$");
            var matcher = pattern.matcher(configuration.nickname);
            if (matcher.find()) {
                int oldNumber = Integer.parseInt(matcher.group(2));
                int newNumber = oldNumber + 1;
                configuration.nickname = matcher.group(1) + " (" + newNumber + ")";
            } else {
                configuration.nickname += " (1)";
            }
        }

        VisionSource source =
                switch (configuration.matchedCameraInfo.type()) {
                    case UsbCamera -> new USBCameraSource(configuration);
                    case ZeroCopyPicam -> new LibcameraGpuSource(configuration);
                    case FileCamera -> new FileVisionSource(configuration);
                };

        if (source.getFrameProvider() == null) {
            logger.error("Frame provider is null?");
        }
        if (source.getSettables() == null) {
            logger.error("Settables are null?");
        }

        return source;
    }

    public List<VisionModule> getVisionModules() {
        return vmm.getModules();
    }
}

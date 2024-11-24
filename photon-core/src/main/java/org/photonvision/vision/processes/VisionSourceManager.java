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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.LibcameraGpuSource;
import org.photonvision.vision.camera.PVCameraDevice;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.camera.UniqueCameraSummary;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);

    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    private final ConcurrentHashMap<String, PVCameraDevice> cameraDeviceMap =
            new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void registerTimedTasks() {
        TimedTaskManager.getInstance().addTask("CameraDeviceExplorer", this::discoverNewDevices, 1000);
    }

    /**
     * Register new camera configs loaded from disk. This will create vision modules for each camera
     * config and start them.
     *
     * @param configs The loaded camera configs.
     */
    public void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        logger.info("Registering loaded camera configs");

        configs.stream()
                .filter(config -> !config.deactivated)
                .map(VisionSourceManager::loadVisionSourceFromCamConfig)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(VisionModuleManager.getInstance()::addSource)
                .forEach(
                        module -> {
                            var config = module.visionSource.cameraConfiguration;
                            cameraDeviceMap.put(config.uniqueName, PVCameraDevice.fromCameraConfig(config));
                            module.start();
                        });

        logger.info("Finished registering loaded camera configs");
    }

    /**
     * Reactivate a previously created vision source
     *
     * @param uniqueName
     */
    public boolean reactivateMatchedCamera(String uniqueName) {
        // Make sure we have an old, currently -inactive- camera around
        var deactivatedConfig =
                Optional.ofNullable(
                        ConfigManager.getInstance().getConfig().getCameraConfigurations().get(uniqueName));
        if (deactivatedConfig.isEmpty() || !deactivatedConfig.get().deactivated) {
            return false;
        }

        // Check if the camera is already in use by another module
        if (VisionModuleManager.getInstance().getModules().stream()
                .anyMatch(module -> module.uniqueName().equals(uniqueName))) {
            return false;
        }

        // transform the camera info all the way to a VisionModule and then start it
        var created =
                deactivatedConfig
                        .flatMap(VisionSourceManager::loadVisionSourceFromCamConfig)
                        .map(VisionModuleManager.getInstance()::addSource)
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
     * Assign a camera that currently has no associated CameraConfiguration loaded
     *
     * @param uniqueName
     */
    public boolean assignUnmatchedCamera(String uniqueName) {
        // Check if the camera is already in use by another module
        final Predicate<PVCameraDevice> isNotUsedInModule =
                info ->
                        VisionModuleManager.getInstance().getModules().stream()
                                .noneMatch(module -> module.uniqueName().equals(uniqueName));
        // transform the camera info all the way to a VisionModule and then start it
        var created =
                Optional.ofNullable(cameraDeviceMap.get(uniqueName))
                        .filter(isNotUsedInModule)
                        // Make sure we aren't going to overwrite
                        .map(
                                info ->
                                        configFromUniqueName(uniqueName)
                                                .orElse(createConfigForCameras(info, uniqueName)))
                        .flatMap(VisionSourceManager::loadVisionSourceFromCamConfig)
                        .map(VisionModuleManager.getInstance()::addSource)
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

    public boolean deactivateVisionSource(String uniqueName) {
        if (cameraDeviceMap.remove(uniqueName) == null) return false;
        var removed =
                VisionModuleManager.getInstance().visionModules.stream()
                        .filter(module -> module.uniqueName().equals(uniqueName))
                        .findFirst()
                        .map(
                                it -> {
                                    VisionModuleManager.getInstance().removeModule(it);
                                    return it;
                                })
                        .isPresent();

        pushUiUpdate();

        return removed;
    }

    // Jackson does use these
    @SuppressWarnings("unused")
    private static class VisionSourceManagerState {
        public List<UniqueCameraSummary> activeCameras;
        public List<UICameraConfiguration> disabledCameras;
        public List<UniqueCameraSummary> allConnectedCameras;
    }

    protected VisionSourceManagerState getVsmState() {
        var ret = new VisionSourceManagerState();

        ret.activeCameras =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().values().stream()
                        .filter(it -> !it.deactivated)
                        .map(it -> new UniqueCameraSummary(it.uniqueName, cameraDeviceMap.get(it.uniqueName)))
                        .toList();
        ret.disabledCameras =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().values().stream()
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

    protected void discoverNewDevices() {
        // Get all connected cameras
        List<PVCameraDevice> seenInfos = filterAllowedDevices(getConnectedCameras());
        Collection<PVCameraDevice> knownInfos = cameraDeviceMap.values();

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

    protected static List<PVCameraDevice> getConnectedCameras() {
        List<PVCameraDevice> cameraInfos = new ArrayList<>();
        // find all connected cameras
        // cscore can return usb and csi cameras but csi are filtered out
        Stream.of(UsbCamera.enumerateUsbCameras())
                .map(c -> PVCameraDevice.fromUsbCameraInfo(c))
                .filter(c -> !(String.join("", c.otherPaths()).contains("csi-video")))
                .filter(c -> !c.name().equals("unicam"))
                .forEach(cameraInfos::add);
        if (LibCameraJNILoader.isSupported()) {
            // find all CSI cameras (Raspberry Pi cameras)
            Stream.of(LibCameraJNI.getCameraNames())
                    .map(
                            path -> {
                                String name = LibCameraJNI.getSensorModel(path).getFriendlyName();
                                return PVCameraDevice.fromCSICameraInfo(path, name);
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

    private static CameraConfiguration createConfigForCameras(
            PVCameraDevice info, String uniqueName) {
        // create new camera config for all new cameras
        logger.info("Creating a new camera config for camera " + uniqueName);

        CameraConfiguration configuration =
                new CameraConfiguration(
                        info.name(), uniqueName, uniqueName, info.path(), info.otherPaths());
        configuration.cameraType = info.type();

        if (info instanceof PVCameraDevice.PVUsbCameraInfo usbInfo) {
            configuration.usbVID = usbInfo.vendorId;
            configuration.usbPID = usbInfo.productId;
        }

        return configuration;
    }

    private static Optional<CameraConfiguration> configFromUniqueName(String uniqueName) {
        return Optional.ofNullable(
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get(uniqueName));
    }

    private static List<PVCameraDevice> filterAllowedDevices(List<PVCameraDevice> allDevices) {
        Platform platform = Platform.getCurrentPlatform();
        ArrayList<PVCameraDevice> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            boolean valid = false;
            if (deviceBlacklist.contains(device.name())) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name() + "\" at \"" + device.path() + "\"");
            } else if (device instanceof PVCameraDevice.PVUsbCameraInfo usbDevice) {
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

    private static Optional<VisionSource> loadVisionSourceFromCamConfig(
            CameraConfiguration configuration) {
        VisionSource source = null;

        if (configuration.cameraType == CameraType.ZeroCopyPicam) {
            // If the camera was loaded from libcamera then create its source using libcamera.
            var piCamSrc = new LibcameraGpuSource(configuration);
            source = piCamSrc;
        } else {
            var newCam = new USBCameraSource(configuration);
            if (!newCam.getCameraQuirks().hasQuirk(CameraQuirk.CompletelyBroken)
                    && !newCam.getSettables().videoModes.isEmpty()) {
                source = newCam;
            }
        }
        if (source == null) {
            logger.error(
                    "Failed to create VisionSource for camera "
                            + configuration.toShortString()
                            + " with type "
                            + configuration.cameraType);
        } else {
            logger.debug("Creating VisionSource for " + configuration.toShortString());
        }
        return Optional.ofNullable(source);
    }
}

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
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.camera.UniqueCameraSummary;

public class VisionSourceManager {
    private static final Logger logger = new Logger(VisionSourceManager.class, LogGroup.Camera);

    private static final List<String> deviceBlacklist = List.of("bcm2835-isp");

    // Jackson does use these members even if your IDE claims otherwise
    static class VisionSourceManagerState {
        public List<UniqueCameraSummary> activeCameras;
        public List<UICameraConfiguration> disabledCameras;
        public List<UniqueCameraSummary> allConnectedCameras;
    }

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public VisionModuleManager vmm = new VisionModuleManager();

    // TODO - delete these. and replace ConcurrentHashMap with syncronization
    private final ConcurrentHashMap<String, CameraConfiguration> deserializedConfigs =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PVCameraInfo> cameraDeviceMap = new ConcurrentHashMap<>();

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
    }

    /**
     * Reactivate a previously created vision source
     *
     * @param uniqueName
     */
    public boolean reactivateMatchedCamera(String uniqueName) {
        // Make sure we have an old, currently -inactive- camera around
        var deactivatedConfig = Optional.ofNullable(this.deserializedConfigs.get(uniqueName));
        if (deactivatedConfig.isEmpty() || !deactivatedConfig.get().deactivated) {
            return false;
        }

        // Check if the camera is already in use by another module
        if (this.deserializedConfigs.keySet().stream().anyMatch(it -> it.equals(uniqueName))) {
            return false;
        }

        // transform the camera info all the way to a VisionModule and then start it
        var created =
                deactivatedConfig
                        .flatMap(this::loadVisionSourceFromCamConfig)
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
     * Assign a camera that currently has no associated CameraConfiguration loaded
     *
     * @param uniqueName
     */
    public boolean assignUnmatchedCamera(String uniqueName) {
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
                        .flatMap(this::loadVisionSourceFromCamConfig)
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

    public boolean deactivateVisionSource(String uniqueName) {
        if (cameraDeviceMap.remove(uniqueName) == null) return false;
        var removed =
                vmm.getModules().stream()
                        .filter(module -> module.uniqueName().equals(uniqueName))
                        .findFirst()
                        .map(
                                it -> {
                                    vmm.removeModule(it);
                                    return it;
                                })
                        .isPresent();

        pushUiUpdate();

        return removed;
    }

    protected VisionSourceManagerState getVsmState() {
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

    protected void discoverNewDevices() {
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

    protected Optional<VisionSource> loadVisionSourceFromCamConfig(
            CameraConfiguration configuration) {
        VisionSource source = null;

        if (configuration.matchedCameraInfo.type() == CameraType.ZeroCopyPicam) {
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
                            + configuration.matchedCameraInfo.type());
        } else {
            logger.debug("Creating VisionSource for " + configuration.toShortString());
        }
        return Optional.ofNullable(source);
    }

    public List<VisionModule> getVisionModules() {
        return vmm.getModules();
    }
}

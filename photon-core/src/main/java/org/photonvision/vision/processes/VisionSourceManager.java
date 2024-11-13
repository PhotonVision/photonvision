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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration;
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
    private String ignoredCamerasRegex = "";

    private final AtomicBoolean configsLoaded = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, PVCameraInfo> cameraInfoMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final VisionSourceManager INSTANCE = new VisionSourceManager();
    }

    public static VisionSourceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    VisionSourceManager() {
        TimedTaskManager.getInstance().addTask("CameraDeviceExplorer", this::discoverNewDevices, 5000);
    }

    void clearConfigState() {
        configsLoaded.set(false);
    }

    /**
     * Register new camera configs loaded from disk. This will create vision modules for each camera
     * config and start them.
     *
     * @param configs The loaded camera configs.
     */
    public void registerLoadedConfigs(Collection<CameraConfiguration> configs) {
        if (!configsLoaded.compareAndSet(false, true)) {
            logger.warn("Attempted to register loaded configs after they were already loaded");
            return;
        }
        logger.info("Registering loaded camera configs");

        configs.stream()
                .map(VisionSourceManager::loadVisionSourceFromCamConfig)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(VisionModuleManager.getInstance()::addSource)
                .forEach(
                        module -> {
                            var config = module.visionSource.cameraConfiguration;
                            cameraInfoMap.put(config.uniqueName, PVCameraInfo.fromCameraConfig(config));
                            module.start();
                        });

        logger.info("Finished registering loaded camera configs");
    }

    public Optional<CameraConfiguration> configureNewVisionSource(String uniqueName) {
        var cfg =
                Optional.ofNullable(cameraInfoMap.get(uniqueName))
                        .filter(
                                u ->
                                        VisionModuleManager.getInstance().getModules().stream()
                                                .noneMatch(module -> module.uniqueName().equals(uniqueName)))
                        .map(info -> createConfigForCameras(info, uniqueName));
        cfg.flatMap(VisionSourceManager::loadVisionSourceFromCamConfig)
                .map(VisionModuleManager.getInstance()::addSource)
                .ifPresent(
                        it -> {
                            it.start();
                            it.saveAndBroadcastAll();
                        });

        return cfg;
    }

    protected List<UniqueCameraSummary> getUniqueUnusedCameras() {
        List<String> activeUniqueNames =
                VisionModuleManager.getInstance().getModules().stream()
                        .map(module -> module.uniqueName())
                        .collect(Collectors.toList());
        return cameraInfoMap.entrySet().stream()
                .filter(entry -> !activeUniqueNames.contains(entry.getKey()))
                .map(entry -> new UniqueCameraSummary(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    protected void discoverNewDevices() {
        if (!configsLoaded.get()) {
            logger.debug("Not discovering new devices because configs are not loaded");
            return;
        }

        int prevSize = cameraInfoMap.size();

        List<PVCameraInfo> devices =
                filterAllowedDevices(getConnectedCameras(), Platform.getCurrentPlatform());

        List<String> infoNames =
                cameraInfoMap.values().stream()
                        .map(ci -> ci.uniquePath() + String.join(",", ci.otherPaths()))
                        .collect(Collectors.toList());
        List<PVCameraInfo> filteredDevices =
                devices.stream()
                        .filter(d -> !infoNames.contains(d.uniquePath() + String.join(",", d.otherPaths())))
                        .collect(Collectors.toList());
        for (var device : filteredDevices) {
            var uniqueName = uniqueName(device.name(), cameraInfoMap.keySet());
            cameraInfoMap.put(uniqueName, device);
        }

        if (prevSize != cameraInfoMap.size()) {
            logger.info("Discovered new devices: " + cameraInfoMap.size());
        }

        // hack - always publish
        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("discoveredCameras", getUniqueUnusedCameras()));
    }

    protected List<PVCameraInfo> getConnectedCameras() {
        List<PVCameraInfo> cameraInfos = new ArrayList<>();
        cameraInfos.addAll(getConnectedUSBCameras());
        cameraInfos.addAll(getConnectedCSICameras());
        return cameraInfos;
    }

    /**
     * Pre filter out any csi cameras to return just USB Cameras. Allow defining the camerainfo.
     *
     * @return a list containing usbcamerainfo.
     */
    protected List<PVCameraInfo> getConnectedUSBCameras() {
        List<PVCameraInfo> cameraInfos =
                List.of(UsbCamera.enumerateUsbCameras()).stream()
                        .map(c -> PVCameraInfo.fromUsbCameraInfo(c))
                        .collect(Collectors.toList());
        return cameraInfos;
    }

    /**
     * Retrieve the list of csi cameras from libcamera.
     *
     * @return a list containing csicamerainfo.
     */
    protected List<PVCameraInfo> getConnectedCSICameras() {
        List<PVCameraInfo> cameraInfos = new ArrayList<PVCameraInfo>();
        if (LibCameraJNILoader.isSupported())
            for (String path : LibCameraJNI.getCameraNames()) {
                String name = LibCameraJNI.getSensorModel(path).getFriendlyName();
                cameraInfos.add(PVCameraInfo.fromCSICameraInfo(path, name));
            }
        return cameraInfos;
    }

    private String uniqueName(String name, Collection<String> takenNames) {
        if (!takenNames.contains(name)) {
            return name;
        }
        int i = 1;
        while (takenNames.contains(name + i)) {
            i++;
        }
        return String.format("%s (%d)", name, i);
    }

    /**
     * Create new {@link CameraConfiguration}s for unmatched cameras, and assign them a unique name
     * (unique in the set of (loaded configs, unloaded configs, loaded vision modules) at least)
     */
    private static CameraConfiguration createConfigForCameras(PVCameraInfo info, String uniqueName) {
        // create new camera config for all new cameras
        String baseName = info.name();

        logger.info("Creating a new camera config for camera " + uniqueName);

        String[] otherPaths = {};
        if (info instanceof PVCameraInfo.PVUsbCameraInfo usbInfo) {
            otherPaths = usbInfo.otherPaths;
        }

        CameraConfiguration configuration =
                new CameraConfiguration(baseName, uniqueName, uniqueName, info.path(), otherPaths);

        configuration.cameraType = info.type();

        return configuration;
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
    private ArrayList<PVCameraInfo> filterAllowedDevices(
            List<PVCameraInfo> allDevices, Platform platform) {
        ArrayList<PVCameraInfo> filteredDevices = new ArrayList<>();
        for (var device : allDevices) {
            boolean valid = false;
            if (deviceBlacklist.contains(device.name())) {
                logger.trace(
                        "Skipping blacklisted device: \"" + device.name() + "\" at \"" + device.path() + "\"");
            } else if (device.name().matches(ignoredCamerasRegex)) {
                logger.trace("Skipping ignored device: \"" + device.name() + "\" at \"" + device.path());
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

    private static Optional<VisionSource> loadVisionSourceFromCamConfig(
            CameraConfiguration configuration) {
        VisionSource source = null;
        boolean is_pi = Platform.isRaspberryPi();

        if (configuration.cameraType == CameraType.ZeroCopyPicam && is_pi) {
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
        logger.debug("Creating VisionSource for " + configuration.toShortString());
        return Optional.ofNullable(source);
    }
}

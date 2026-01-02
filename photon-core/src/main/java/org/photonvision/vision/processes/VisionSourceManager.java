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
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.dataflow.websocket.UICameraConfiguration;
import org.photonvision.common.dataflow.websocket.UIPhotonConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.Platform.OSType;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.DuplicateVisionSource;
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

    // Track which duplicates depend on which sources
    // Key: source uniqueName, Value: list of duplicate uniqueNames
    private final HashMap<String, List<String>> duplicateDependencies = new HashMap<>();

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
            } else if (config.matchedCameraInfo.type() != CameraType.DuplicateCamera
                    && deserializedConfigs.values().stream()
                            .map(it -> it.matchedCameraInfo)
                            .anyMatch(checkDuplicateCamera)) {
                // Skip uniquePath check for duplicate cameras - multiple duplicates of the same source
                // are intentional and should all be loaded
                logger.error(
                        "Duplicate camera type & path for config " + config.uniqueName + " -- not overwriting");
            } else {
                deserializedConfigs.put(config.uniqueName, config);
            }
        }

        // 2. create sources -> VMMs for all active cameras and add to our VMM. We don't care about if
        // the underlying device is currently connected or not.
        // IMPORTANT: Process non-duplicate cameras first, so duplicate cameras can find their sources
        deserializedConfigs.values().stream()
                .filter(it -> !it.deactivated)
                .sorted(
                        (a, b) -> {
                            boolean aIsDuplicate =
                                    a.matchedCameraInfo instanceof PVCameraInfo.PVDuplicateCameraInfo;
                            boolean bIsDuplicate =
                                    b.matchedCameraInfo instanceof PVCameraInfo.PVDuplicateCameraInfo;
                            return Boolean.compare(
                                    aIsDuplicate, bIsDuplicate); // false < true, so non-duplicates first
                        })
                .map(this::loadVisionSourceFromCamConfig)
                .filter(it -> it != null) // Filter out null sources (e.g., duplicates with missing source)
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

        // If this is a duplicate, check if source is available
        if (deactivatedConfig.get().matchedCameraInfo
                instanceof PVCameraInfo.PVDuplicateCameraInfo dupInfo) {
            var sourceModule =
                    vmm.getModules().stream()
                            .filter(m -> m.uniqueName().equals(dupInfo.sourceUniqueName))
                            .findFirst()
                            .orElse(null);
            if (sourceModule == null) {
                logger.error(
                        "Cannot reactivate duplicate - source camera not active: " + dupInfo.sourceUniqueName);
                // Restore state
                this.disabledCameraConfigs.put(uniqueName, deactivatedConfig.get());
                return false;
            }
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

        // Track if this is a source camera so we can reactivate duplicates afterward
        boolean isSourceCamera =
                !(deactivatedConfig.get().matchedCameraInfo instanceof PVCameraInfo.PVDuplicateCameraInfo);

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
        } else if (isSourceCamera) {
            // This was a source camera - try to reactivate its duplicates
            handleSourceCameraReactivation(uniqueName);
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

        // Check if this new camera is a source for any disabled duplicates
        handleSourceCameraReactivation(module.uniqueName());

        // We have a new camera! Tell the world about it
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));

        pushUiUpdate();

        return true;
    }

    /**
     * Create a duplicate of an existing active camera
     *
     * @param sourceUniqueName The unique name of the source camera to duplicate
     * @return The unique name of the created duplicate, or null if failed
     */
    public synchronized String createDuplicateCamera(String sourceUniqueName) {
        // 1. Find the source VisionModule
        var sourceModule =
                vmm.getModules().stream()
                        .filter(module -> module.uniqueName().equals(sourceUniqueName))
                        .findFirst()
                        .orElse(null);

        if (sourceModule == null) {
            logger.error("Cannot create duplicate - source camera not found: " + sourceUniqueName);
            return null;
        }

        // 2. Create a new configuration for the duplicate
        var sourceConfig = sourceModule.getCameraConfiguration();
        var duplicateUniqueName = java.util.UUID.randomUUID().toString();

        // Generate a unique nickname by checking existing names
        var currentNicknames = new ArrayList<String>();
        this.disabledCameraConfigs.values().stream()
                .map(it -> it.nickname)
                .forEach(currentNicknames::add);
        this.vmm.getModules().stream()
                .map(it -> it.getCameraConfiguration().nickname)
                .forEach(currentNicknames::add);

        // Find a unique duplicate name
        String duplicateNickname = sourceConfig.nickname + " (Duplicate)";
        int duplicateNumber = 2;
        while (currentNicknames.contains(duplicateNickname)) {
            duplicateNickname = sourceConfig.nickname + " (Duplicate " + duplicateNumber + ")";
            duplicateNumber++;
        }

        var duplicateCameraInfo =
                new PVCameraInfo.PVDuplicateCameraInfo(sourceUniqueName, duplicateNickname);

        var duplicateConfig = new CameraConfiguration(duplicateUniqueName, duplicateCameraInfo);

        // Set the nickname explicitly
        duplicateConfig.nickname = duplicateNickname;

        // Copy FOV and quirks from source
        duplicateConfig.FOV = sourceConfig.FOV;
        duplicateConfig.cameraQuirks = sourceConfig.cameraQuirks;

        // 3. Create the duplicate vision source
        var duplicateSource = new DuplicateVisionSource(duplicateConfig, sourceModule.visionSource);

        // 4. Add to VMM and start
        var duplicateModule = vmm.addSource(duplicateSource);
        duplicateModule.start();

        // 5. Track the dependency relationship
        duplicateDependencies
                .computeIfAbsent(sourceUniqueName, k -> new ArrayList<>())
                .add(duplicateUniqueName);

        // 6. Save and broadcast
        duplicateModule.saveAndBroadcastAll();

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));
        pushUiUpdate();

        logger.info(
                "Created duplicate camera '"
                        + duplicateConfig.nickname
                        + "' from source '"
                        + sourceConfig.nickname
                        + "'");

        return duplicateUniqueName;
    }

    public synchronized boolean deleteVisionSource(String uniqueName) {
        // If this is a source camera with duplicates, delete all duplicates
        var dependentDuplicates = duplicateDependencies.get(uniqueName);
        if (dependentDuplicates != null && !dependentDuplicates.isEmpty()) {
            logger.info(
                    "Deleting source camera "
                            + uniqueName
                            + ". Deleting "
                            + dependentDuplicates.size()
                            + " dependent duplicate(s)");

            // Deactivate all dependent duplicates
            for (var duplicateUniqueName : new ArrayList<>(dependentDuplicates)) {
                deleteVisionSource(duplicateUniqueName);
            }

            duplicateDependencies.remove(uniqueName);
        }

        // Deactivate if active, then remove from config
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

    /** Called when a source camera is deactivated. Deactivates all dependent duplicates. */
    private synchronized void handleSourceCameraRemoval(String sourceUniqueName) {
        var dependentDuplicates = duplicateDependencies.get(sourceUniqueName);
        if (dependentDuplicates == null || dependentDuplicates.isEmpty()) {
            return;
        }

        logger.info(
                "Source camera "
                        + sourceUniqueName
                        + " deactivated. Deactivating "
                        + dependentDuplicates.size()
                        + " dependent duplicate(s)");

        // Deactivate all dependent duplicates
        for (var duplicateUniqueName : new ArrayList<>(dependentDuplicates)) {
            deactivateVisionSource(duplicateUniqueName);
        }

        duplicateDependencies.remove(sourceUniqueName);
    }

    /** Called when a source camera is reactivated. Attempts to reactivate dependent duplicates. */
    private synchronized void handleSourceCameraReactivation(String sourceUniqueName) {
        // Find all disabled duplicates that reference this source
        var duplicatesToReactivate =
                disabledCameraConfigs.values().stream()
                        .filter(
                                config -> config.matchedCameraInfo instanceof PVCameraInfo.PVDuplicateCameraInfo)
                        .filter(
                                config ->
                                        ((PVCameraInfo.PVDuplicateCameraInfo) config.matchedCameraInfo)
                                                .sourceUniqueName.equals(sourceUniqueName))
                        .map(config -> config.uniqueName)
                        .toList();

        if (duplicatesToReactivate.isEmpty()) {
            return;
        }

        logger.info(
                "Source camera "
                        + sourceUniqueName
                        + " reactivated. Reactivating "
                        + duplicatesToReactivate.size()
                        + " dependent duplicate(s)");

        for (var duplicateUniqueName : duplicatesToReactivate) {
            reactivateDisabledCameraConfig(duplicateUniqueName);
        }
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

        // Check if this was a source camera with duplicates
        handleSourceCameraRemoval(uniqueName);

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
        if (LoadJNI.hasLoaded(JNITypes.LIBCAMERA)) {
            // find all CSI cameras (Raspberry Pi cameras)
            Stream.of(LibCameraJNI.getCameraNames())
                    .map(
                            path -> {
                                String name = LibCameraJNI.getSensorModel(path).getFriendlyName();
                                return PVCameraInfo.fromCSICameraInfo(path, name);
                            })
                    .forEach(cameraInfos::add);
        }

        // FileVisionSources are a bit quirky. They aren't enumerated by the above, but I still want my
        // UI to look like it ought to work
        vmm.getModules().stream()
                .map(it -> it.getCameraConfiguration().matchedCameraInfo)
                .filter(info -> info instanceof PVCameraInfo.PVFileCameraInfo)
                .forEach(cameraInfos::add);

        checkMismatches(cameraInfos);

        return cameraInfos;
    }

    /**
     * Check for mismatches between connected cameras and saved camera configurations.
     *
     * <p>Note that if the information for a camera spontaneously changes without it being
     * disconnected/unplugged and reconnected/replugged, we may experience unexpected behavior.
     *
     * @param cameraInfos List of currently connected camera infos, checked against saved configs
     */
    protected void checkMismatches(List<PVCameraInfo> cameraInfos) {
        // from the listed physical camera infos, match them to the camera configs and check for
        // mismatches
        for (VisionModule module : vmm.getModules()) {
            PVCameraInfo matchedCameraInfo = module.getCameraConfiguration().matchedCameraInfo;
            // We use unique paths to determine if the module has a camera in the port. If no unique path
            // is found that matches the module, it's removed from the mismatched set as a disconnected
            // camera cannot be mismatched.
            if (!cameraInfos.stream()
                    .map(PVCameraInfo::uniquePath)
                    .toList()
                    .contains(matchedCameraInfo.uniquePath())) {
                module.mismatch = false;
                continue;
            }

            for (PVCameraInfo info : cameraInfos) {
                // if the unique path doesn't match, skip cause it's not in the same port
                if (!matchedCameraInfo.uniquePath().equals(info.uniquePath())) {
                    continue;
                }

                // If the camera info doesn't match, log an error
                if (!matchedCameraInfo.equals(info) && !module.mismatch) {
                    logger.error("Camera mismatch error!");
                    logger.error("Camera config mismatch for " + matchedCameraInfo.name());
                    logCameraInfoDiff(matchedCameraInfo, info);
                    module.mismatch = true;
                }
            }
        }

        // Set the NetworkTables mismatch alert
        if (vmm.getModules().stream().anyMatch(m -> m.mismatch)) {
            NetworkTablesManager.getInstance()
                    .setMismatchAlert(
                            true,
                            "Camera mismatch error! See logs for details. ("
                                    + vmm.getModules().stream()
                                            .filter(m -> m.mismatch)
                                            .map(m -> m.getCameraConfiguration().nickname)
                                            .toList()
                                            .toString()
                                            .replaceAll("[\\[\\]()]", "")
                                    + " affected)");
        } else {
            NetworkTablesManager.getInstance().setMismatchAlert(false, "");
        }
    }

    /** Log the differences between two PVCameraInfo objects. */
    private static void logCameraInfoDiff(PVCameraInfo saved, PVCameraInfo current) {
        String expected = "Expected: Name: " + saved.name();
        String actual = "Actual: Name: " + current.name();
        if (saved instanceof PVCameraInfo.PVCSICameraInfo savedCsi
                && current instanceof PVCameraInfo.PVCSICameraInfo currentCsi) {
            expected += " Base Name: " + savedCsi.baseName;
            actual += " Base Name: " + currentCsi.baseName;
        }

        expected += " Type: " + saved.type().toString();
        actual += " Type: " + current.type().toString();

        if (saved instanceof PVCameraInfo.PVUsbCameraInfo savedUsb
                && current instanceof PVCameraInfo.PVUsbCameraInfo currentUsb) {
            expected +=
                    " Device Number: "
                            + savedUsb.dev
                            + " Vendor ID: "
                            + savedUsb.vendorId
                            + " Product ID: "
                            + savedUsb.productId;
            actual +=
                    " Device Number: "
                            + currentUsb.dev
                            + " Vendor ID: "
                            + currentUsb.vendorId
                            + " Product ID: "
                            + currentUsb.productId;
        }

        expected += " Path: " + saved.path();
        actual += " Path: " + current.path();
        expected += " Unique Path: " + saved.uniquePath();
        actual += " Unique Path: " + current.uniquePath();
        expected += " Other Paths: " + Arrays.toString(saved.otherPaths());
        actual += " Other Paths: " + Arrays.toString(current.otherPaths());

        logger.error(expected);
        logger.error(actual);
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
                    case DuplicateCamera -> {
                        // This is a duplicate camera - find the source and wrap it
                        var dupInfo = (PVCameraInfo.PVDuplicateCameraInfo) configuration.matchedCameraInfo;
                        var sourceModule =
                                vmm.getModules().stream()
                                        .filter(m -> m.uniqueName().equals(dupInfo.sourceUniqueName))
                                        .findFirst()
                                        .orElse(null);

                        if (sourceModule == null) {
                            logger.warn(
                                    "Source camera not found for duplicate "
                                            + configuration.nickname
                                            + " (source: "
                                            + dupInfo.sourceUniqueName
                                            + "). Marking as deactivated.");
                            configuration.deactivated = true;
                            disabledCameraConfigs.put(configuration.uniqueName, configuration);
                            yield null;
                        }

                        // Create duplicate vision source and track dependency
                        var duplicateSource =
                                new DuplicateVisionSource(configuration, sourceModule.visionSource);
                        duplicateDependencies
                                .computeIfAbsent(dupInfo.sourceUniqueName, k -> new ArrayList<>())
                                .add(configuration.uniqueName);

                        yield duplicateSource;
                    }
                };

        if (source == null) {
            return null; // Duplicate with missing source
        }

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

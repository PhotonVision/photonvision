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

package org.photonvision.common.configuration;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import java.util.*;
// import java.util.stream.Collectors;
// import org.photonvision.PhotonVersion;
// import org.photonvision.common.hardware.Platform;
// import org.photonvision.common.networking.NetworkManager;
// import org.photonvision.common.networking.NetworkUtils;
// import org.photonvision.common.util.SerializationUtils;
// import org.photonvision.jni.RknnDetectorJNI;
// import org.photonvision.mrcal.MrCalJNILoader;
// import org.photonvision.raspi.LibCameraJNILoader;
// import org.photonvision.vision.processes.VisionModule;
// import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;

public class PhotonConfiguration {
    private HardwareConfig hardwareConfig;
    private HardwareSettings hardwareSettings;
    private NetworkConfig networkConfig;
    private MiscellaneousSettings miscSettings;
    private AprilTagFieldLayout atfl;

    private final HashMap<String, CameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            MiscellaneousSettings miscSettings,
            AprilTagFieldLayout atfl) {
        this(hardwareConfig, hardwareSettings, networkConfig, miscSettings, atfl, new HashMap<>());
    }

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            MiscellaneousSettings miscSettings,
            AprilTagFieldLayout atfl,
            HashMap<String, CameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;
        this.networkConfig = networkConfig;
        this.miscSettings = miscSettings;
        this.cameraConfigurations = cameraConfigurations;
        this.atfl = atfl;
    }

    public PhotonConfiguration() {
        this(
                new HardwareConfig(),
                new HardwareSettings(),
                new NetworkConfig(),
                new MiscellaneousSettings(),
                new AprilTagFieldLayout(List.of(), 0, 0));
    }

    public HardwareConfig getHardwareConfig() {
        return hardwareConfig;
    }

    public void setHardwareConfig(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
    }

    public HardwareSettings getHardwareSettings() {
        return hardwareSettings;
    }

    public void setHardwareSettings(HardwareSettings hardwareSettings) {
        this.hardwareSettings = hardwareSettings;
    }

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    public MiscellaneousSettings getMiscSettings() {
        return miscSettings;
    }

    public void setMiscSettings(MiscellaneousSettings miscSettings) {
        this.miscSettings = miscSettings;
    }

    public AprilTagFieldLayout getApriltagFieldLayout() {
        return atfl;
    }

    public void setApriltagFieldLayout(AprilTagFieldLayout atfl) {
        this.atfl = atfl;
    }

    public HashMap<String, CameraConfiguration> getCameraConfigurations() {
        return cameraConfigurations;
    }

    public void addCameraConfigs(Collection<VisionSource> sources) {
        for (var s : sources) {
            addCameraConfig(s.getCameraConfiguration());
        }
    }

    public void addCameraConfig(CameraConfiguration config) {
        addCameraConfig(config.uniqueName, config);
    }

    public void addCameraConfig(String name, CameraConfiguration config) {
        cameraConfigurations.put(name, config);
    }

    //    public Map<String, Object> toHashMap() {
    //        Map<String, Object> retMap = new HashMap<>();
    //
    //        // Instance Config
    //        var instanceConfigSubmap = new HashMap<String, Object>();
    //        instanceConfigSubmap.put("version", PhotonVersion.versionString);
    //        instanceConfigSubmap.put("gpuAccelerationSupported", LibCameraJNILoader.isSupported());
    //        instanceConfigSubmap.put("mrCalWorking", MrCalJNILoader.getInstance().isLoaded());
    //        instanceConfigSubmap.put("rknnSupported", RknnDetectorJNI.getInstance().isLoaded());
    //        instanceConfigSubmap.put("hardwareModel", hardwareConfig.deviceName);
    //        instanceConfigSubmap.put("hardwarePlatform", Platform.getPlatformName());
    //        retMap.put("instanceConfig", instanceConfigSubmap);
    //
    //        var settingsSubmap = new HashMap<String, Object>();
    //
    //        // Lighting Settings
    //        var lightingSettingsSubmap = new HashMap<String, Object>();
    //        lightingSettingsSubmap.put("brightness", hardwareSettings.ledBrightnessPercentage);
    //        lightingSettingsSubmap.put("supported", !hardwareConfig.ledPins.isEmpty());
    //        settingsSubmap.put("lighting", lightingSettingsSubmap);
    //
    //        // Network Settings
    //        var networkSettingsSubmap = networkConfig.toHashMap();
    //        networkSettingsSubmap.put("networkInterfaceNames",
    // NetworkUtils.getAllWiredInterfaces());
    //        networkSettingsSubmap.put("networkingDisabled",
    // NetworkManager.getInstance().networkingIsDisabled);
    //        settingsSubmap.put("network", networkSettingsSubmap);
    //
    //        // Misc Settings
    //        var miscSettingsSubmap = SerializationUtils.objectToHashMap(miscSettings);
    //        settingsSubmap.put("misc", miscSettingsSubmap);
    //
    //        retMap.put("settings", settingsSubmap);
    //
    //        retMap.put("activeATFL", this.atfl);
    //
    //        retMap.put(
    //            "cameras",
    //            VisionModuleManager.getInstance().getModules().stream()
    //                    .map(VisionModule::toUIHashMap)
    //                    .collect(Collectors.toList()));
    //
    //        return retMap;
    //    }
}

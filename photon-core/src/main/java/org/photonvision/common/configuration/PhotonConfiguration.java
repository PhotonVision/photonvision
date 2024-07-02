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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.photonvision.PhotonVersion;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.jni.RknnDetectorJNI;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.calibration.UICameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;

public class PhotonConfiguration {
    private final HardwareConfig hardwareConfig;
    private final HardwareSettings hardwareSettings;
    private NetworkConfig networkConfig;
    private AprilTagFieldLayout atfl;
    private HashMap<String, CameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            AprilTagFieldLayout atfl) {
        this(hardwareConfig, hardwareSettings, networkConfig, atfl, new HashMap<>());
    }

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            AprilTagFieldLayout atfl,
            HashMap<String, CameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;
        this.networkConfig = networkConfig;
        this.cameraConfigurations = cameraConfigurations;
        this.atfl = atfl;
    }

    public PhotonConfiguration() {
        this(
                new HardwareConfig(),
                new HardwareSettings(),
                new NetworkConfig(),
                new AprilTagFieldLayout(List.of(), 0, 0));
    }

    public HardwareConfig getHardwareConfig() {
        return hardwareConfig;
    }

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public HardwareSettings getHardwareSettings() {
        return hardwareSettings;
    }

    public AprilTagFieldLayout getApriltagFieldLayout() {
        return atfl;
    }

    public void setApriltagFieldLayout(AprilTagFieldLayout atfl) {
        this.atfl = atfl;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
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

    public Map<String, Object> toHashMap() {
        Map<String, Object> map = new HashMap<>();
        var settingsSubmap = new HashMap<String, Object>();

        // Hack active interfaces into networkSettings
        var netConfigMap = networkConfig.toHashMap();
        netConfigMap.put("networkInterfaceNames", NetworkUtils.getAllWiredInterfaces());
        netConfigMap.put("networkingDisabled", NetworkManager.getInstance().networkingIsDisabled);

        settingsSubmap.put("networkSettings", netConfigMap);

        var lightingConfig = new UILightingConfig();
        lightingConfig.brightness = hardwareSettings.ledBrightnessPercentage;
        lightingConfig.supported = !hardwareConfig.ledPins.isEmpty();
        settingsSubmap.put("lighting", SerializationUtils.objectToHashMap(lightingConfig));
        // General Settings
        var generalSubmap = new HashMap<String, Object>();
        generalSubmap.put("version", PhotonVersion.versionString);
        generalSubmap.put(
                "gpuAcceleration",
                LibCameraJNILoader.isSupported()
                        ? "Zerocopy Libcamera Working"
                        : ""); // TODO add support for other types of GPU accel
        generalSubmap.put("mrCalWorking", MrCalJNILoader.getInstance().isLoaded());
        generalSubmap.put("rknnSupported", RknnDetectorJNI.getInstance().isLoaded());
        generalSubmap.put("availableRknnModels", NeuralNetworkModelManager.getInstance().getModels());
        generalSubmap.put("hardwareModel", hardwareConfig.deviceName);
        generalSubmap.put("hardwarePlatform", Platform.getPlatformName());
        settingsSubmap.put("general", generalSubmap);
        // AprilTagFieldLayout
        settingsSubmap.put("atfl", this.atfl);

        map.put(
                "cameraSettings",
                VisionModuleManager.getInstance().getModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .map(SerializationUtils::objectToHashMap)
                        .collect(Collectors.toList()));
        map.put("settings", settingsSubmap);

        return map;
    }

    public static class UILightingConfig {
        public int brightness = 0;
        public boolean supported = true;
    }

    public static class UICameraConfiguration {
        @SuppressWarnings("unused")
        public double fov;

        public String nickname;
        public String uniqueName;
        public HashMap<String, Object> currentPipelineSettings;
        public int currentPipelineIndex;
        public List<String> pipelineNicknames;
        public HashMap<Integer, HashMap<String, Object>> videoFormatList;
        public int outputStreamPort;
        public int inputStreamPort;
        public List<UICameraCalibrationCoefficients> calibrations;
        public boolean isFovConfigurable = true;
        public QuirkyCamera cameraQuirks;
        public boolean isCSICamera;
    }
}

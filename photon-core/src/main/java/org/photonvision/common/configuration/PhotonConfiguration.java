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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;

// TODO rename this class
public class PhotonConfiguration {

    private HardwareConfig hardwareConfig;
    private HardwareSettings hardwareSettings;
    private NetworkConfig networkConfig;
    private HashMap<String, CameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig) {
        this(hardwareConfig, hardwareSettings, networkConfig, new HashMap<>());
    }

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            HashMap<String, CameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;
        this.networkConfig = networkConfig;
        this.cameraConfigurations = cameraConfigurations;
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

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    public HashMap<String, CameraConfiguration> getCameraConfigurations() {
        return cameraConfigurations;
    }

    public void addCameraConfigs(Collection<CameraConfiguration> config) {
        for (var c : config) {
            addCameraConfig(c);
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

        settingsSubmap.put("networkSettings", networkConfig.toHashMap());
        map.put(
                "cameraSettings",
                VisionModuleManager.getInstance().getModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .map(SerializationUtils::objectToHashMap)
                        .collect(Collectors.toList()));

        var lightingConfig = new UILightingConfig();
        lightingConfig.brightness = hardwareSettings.ledBrightnessPercentage;
        lightingConfig.supported = (hardwareConfig.ledPins.size() != 0);
        settingsSubmap.put("lighting", SerializationUtils.objectToHashMap(lightingConfig));

        var generalSubmap = new HashMap<String, Object>();
        // TODO: MONOREPO - global repo versioning!!!!
        generalSubmap.put("version", /*PhotonVersion.versionString*/ "");
        generalSubmap.put(
                "gpuAcceleration",
                PicamJNI.isSupported()
                        ? "Zerocopy MMAL on " + PicamJNI.getSensorModel().getFriendlyName()
                        : ""); // TODO add support for other types of GPU accel
        generalSubmap.put("hardwareModel", hardwareConfig.deviceName);
        generalSubmap.put("hardwarePlatform", Platform.getCurrentPlatform().toString());
        settingsSubmap.put("general", generalSubmap);

        map.put("settings", settingsSubmap);
        return map;
    }

    public static class UILightingConfig {
        public int brightness = 0;
        public boolean supported = true;
    }

    public static class UICameraConfiguration {
        @SuppressWarnings("unused")
        public double fov, tiltDegrees;
        public String nickname;
        public HashMap<String, Object> currentPipelineSettings;
        public int currentPipelineIndex;
        public List<String> pipelineNicknames;
        public HashMap<Integer, HashMap<String, Object>> videoFormatList;
        public int outputStreamPort;
        public int inputStreamPort;
        public List<HashMap<String, Object>> calibrations;
        public boolean isFovConfigurable = true;
    }
}

/*
 * Copyright (C) 2020 Photon Vision.
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

import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;

import javax.sql.rowset.serial.SerialJavaObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO rename this class
public class PhotonConfiguration {
    public HardwareConfig getHardwareConfig() {
        return hardwareConfig;
    }

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public HashMap<String, CameraConfiguration> getCameraConfigurations() {
        return cameraConfigurations;
    }

    public void addCameraConfigs(List<CameraConfiguration> config) {
        for(var c: config) {
            addCameraConfig(c);
        }
    }

    public void addCameraConfig(CameraConfiguration config) {
        addCameraConfig(config.uniqueName, config);
    }

    public void addCameraConfig(String name, CameraConfiguration config) {
        cameraConfigurations.put(name, config);
    }

    private HardwareConfig hardwareConfig;
    private NetworkConfig networkConfig;

    private HashMap<String, CameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(HardwareConfig hardwareConfig, NetworkConfig networkConfig) {
        this(hardwareConfig, networkConfig, new HashMap<>());
    }

    public PhotonConfiguration(
        HardwareConfig hardwareConfig,
        NetworkConfig networkConfig,
        HashMap<String, CameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.networkConfig = networkConfig;
        this.cameraConfigurations = cameraConfigurations;
    }

    public Map<String, Object> toHashMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("networkSettings", networkConfig.toHashMap());
        map.put("cameraSettings", VisionModuleManager.getInstance()
            .getModules().stream().map(VisionModule::toUICameraConfig)
            .map(SerializationUtils::objectToHashMap).collect(Collectors.toList()));

        return map;
    }

    public static class UICameraConfiguration {
        @SuppressWarnings("unused")
        public double fov, tiltDegrees;
        public String nickname;
        public HashMap<String, Object> currentPipelineSettings;
        public int currentPipelineIndex;
        public List<String> pipelineNicknames;
        public HashMap<Integer, HashMap<String, Object>> videoFormatList;
        public int streamPort;
    }

}

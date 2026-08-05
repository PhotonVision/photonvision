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

import io.avaje.jsonb.Json;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.photonvision.vision.processes.VisionSource;
import org.wpilib.vision.apriltag.AprilTagFieldLayout;

@Json
public class PhotonConfiguration {
    private final HardwareConfig hardwareConfig;
    private final HardwareSettings hardwareSettings;
    private NetworkConfig networkConfig;

    @Json.Property("atfl")
    private AprilTagFieldLayout aprilTagFieldLayout;

    private NeuralNetworkModelsSettings neuralNetworkProperties;
    private Map<String, CameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            AprilTagFieldLayout atfl,
            NeuralNetworkModelsSettings neuralNetworkProperties) {
        this(
                hardwareConfig,
                hardwareSettings,
                networkConfig,
                atfl,
                neuralNetworkProperties,
                new HashMap<>());
    }

    @Json.Creator
    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            HardwareSettings hardwareSettings,
            NetworkConfig networkConfig,
            AprilTagFieldLayout atfl,
            NeuralNetworkModelsSettings neuralNetworkProperties,
            Map<String, CameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;
        this.networkConfig = networkConfig;
        this.neuralNetworkProperties = neuralNetworkProperties;
        this.cameraConfigurations = cameraConfigurations;
        this.aprilTagFieldLayout = atfl;
    }

    public PhotonConfiguration() {
        this(
                new HardwareConfig(),
                new HardwareSettings(),
                new NetworkConfig(),
                new AprilTagFieldLayout(List.of(), 0, 0),
                new NeuralNetworkModelsSettings());
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
        return aprilTagFieldLayout;
    }

    public NeuralNetworkModelsSettings getNeuralNetworkProperties() {
        return neuralNetworkProperties;
    }

    public void setApriltagFieldLayout(AprilTagFieldLayout atfl) {
        this.aprilTagFieldLayout = atfl;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    public void setNeuralNetworkProperties(NeuralNetworkModelsSettings neuralNetworkProperties) {
        this.neuralNetworkProperties = neuralNetworkProperties;
    }

    public Map<String, CameraConfiguration> getCameraConfigurations() {
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

    /**
     * Delete a camera by its unique name
     *
     * @param name The camera name (usually unique name)
     * @return True if the camera configuration was removed
     */
    public boolean removeCameraConfig(String name) {
        return cameraConfigurations.remove(name) != null;
    }

    @Override
    public String toString() {
        StringBuilder cameraConfigurationsString = new StringBuilder();
        cameraConfigurations.forEach(
                (key, value) -> {
                    cameraConfigurationsString
                            .append("\n    ")
                            .append(key)
                            .append(" -> ")
                            .append(value.toString());
                });

        return "PhotonConfiguration [\n  hardwareConfig="
                + hardwareConfig
                + "\n  hardwareSettings="
                + hardwareSettings
                + "\n  networkConfig="
                + networkConfig
                + "\n  aprilTagFieldLayout="
                + aprilTagFieldLayout
                + "\n  neuralNetworkProperties="
                + neuralNetworkProperties
                + "\n  cameraConfigurations={"
                + cameraConfigurationsString
                + "}\n]";
    }
}

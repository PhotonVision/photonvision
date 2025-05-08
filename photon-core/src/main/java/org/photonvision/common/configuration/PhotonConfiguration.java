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
        return "PhotonConfiguration [\n  hardwareConfig="
                + hardwareConfig
                + "\n  hardwareSettings="
                + hardwareSettings
                + "\n  networkConfig="
                + networkConfig
                + "\n  atfl="
                + atfl
                + "\n  cameraConfigurations="
                + cameraConfigurations
                + "\n]";
    }
}

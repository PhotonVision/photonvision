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

import java.util.HashMap;

// TODO rename this class
public class PhotonConfiguration {
    public HardwareConfig getHardwareConfig() {
        return hardwareConfig;
    }

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public HashMap<String, USBCameraConfiguration> getCameraConfigurations() {
        return cameraConfigurations;
    }

    public void addCameraConfig(USBCameraConfiguration config) {
        addCameraConfig(config.uniqueName, config);
    }

    public void addCameraConfig(String name, USBCameraConfiguration config) {
        cameraConfigurations.put(name, config);
    }

    private HardwareConfig hardwareConfig;
    private NetworkConfig networkConfig;

    private HashMap<String, USBCameraConfiguration> cameraConfigurations;

    public PhotonConfiguration(HardwareConfig hardwareConfig, NetworkConfig networkConfig) {
        this(hardwareConfig, networkConfig, new HashMap<>());
    }

    public PhotonConfiguration(
            HardwareConfig hardwareConfig,
            NetworkConfig networkConfig,
            HashMap<String, USBCameraConfiguration> cameraConfigurations) {
        this.hardwareConfig = hardwareConfig;
        this.networkConfig = networkConfig;
        this.cameraConfigurations = cameraConfigurations;
    }
}

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

    public HashMap<String, CameraConfiguration> getCameraConfigurations() {
        return cameraConfigurations;
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
}

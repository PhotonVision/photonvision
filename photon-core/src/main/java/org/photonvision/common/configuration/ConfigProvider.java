package org.photonvision.common.configuration;

import java.nio.file.Path;

public abstract class ConfigProvider {
    private PhotonConfiguration config;
    abstract void load();
    void saveToDisk() {};
    PhotonConfiguration getConfig() { return config; }


    abstract public void saveUploadedHardwareConfig(Path uploadPath);
    public abstract void saveUploadedHardwareSettings(Path uploadPath) ;
    public abstract void saveUploadedNetworkConfig(Path uploadPath);
}

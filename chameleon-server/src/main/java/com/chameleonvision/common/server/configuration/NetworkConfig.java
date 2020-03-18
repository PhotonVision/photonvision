package com.chameleonvision.common.server.configuration;

import com.chameleonvision.common.configuration.ConfigFile;
import com.chameleonvision.common.network.NetworkMode;

public class NetworkConfig extends ConfigFile {

    public NetworkMode networkMode = NetworkMode.DHCP;
    public String ip = "";
    public String hostname = "chameleon-vision";

    private NetworkConfig() {
        super("network");
    }

    private static class SingletonHolder {
        private static final NetworkConfig INSTANCE = new NetworkConfig();
    }

    public static NetworkConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

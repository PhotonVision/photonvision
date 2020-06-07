package com.chameleonvision.common.configuration;

import com.chameleonvision.common.networking.NetworkMode;

public class NetworkConfig {
    public int teamNumber = 1577;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String ip = "";
    public String gateway = "";
    public String netmask = "";
    public String hostname = "chameleon-vision";
}

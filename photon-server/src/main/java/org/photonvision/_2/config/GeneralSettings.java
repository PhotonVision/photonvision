package org.photonvision._2.config;

import org.photonvision.common.networking.NetworkMode;

public class GeneralSettings {
    public int teamNumber = 1577;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String ip = "";
    public String gateway = "";
    public String netmask = "";
    public String hostname = "Chameleon-vision";
    public String currentCamera = "";
    public Integer currentPipeline = null;
}

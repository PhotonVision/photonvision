package org.photonvision.common.dataflow.websocket;

import java.util.List;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.networking.NetworkUtils.NMDeviceInfo;

public class UINetConfig extends NetworkConfig {
    public UINetConfig(
            NetworkConfig config, List<NMDeviceInfo> networkInterfaceNames, boolean networkingDisabled) {
        super(config);
        this.networkInterfaceNames = networkInterfaceNames;
        this.networkingDisabled = networkingDisabled;
    }

    public List<NMDeviceInfo> networkInterfaceNames;
    public boolean networkingDisabled;
}

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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkMode;

public class NetworkConfig {
    // Can be an integer team number, or an IP address
    public String ntServerAddress = "0";
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String staticIp = "";
    public String hostname = "photonvision";
    public boolean runNTServer = false;
    public boolean shouldManage;
    public boolean shouldPublishProto = false;

    @JsonIgnore public static final String NM_IFACE_STRING = "${interface}";
    @JsonIgnore public static final String NM_IP_STRING = "${ipaddr}";

    public String networkManagerIface = "";
    // TODO: remove these strings if no longer needed
    public String setStaticCommand =
            "nmcli con mod ${interface} ipv4.addresses ${ipaddr}/8 ipv4.method \"manual\" ipv6.method \"disabled\"";
    public String setDHCPcommand =
            "nmcli con mod ${interface} ipv4.method \"auto\" ipv6.method \"disabled\"";

    public NetworkConfig() {
        // We can (usually) manage networking on Linux devices, and if we can, we should try to. Command
        // line inhibitions happen at a level above this class
        setShouldManage(deviceCanManageNetwork());
    }

    @JsonCreator
    public NetworkConfig(
            @JsonProperty("ntServerAddress") @JsonAlias({"ntServerAddress", "teamNumber"})
                    String ntServerAddress,
            @JsonProperty("connectionType") NetworkMode connectionType,
            @JsonProperty("staticIp") String staticIp,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("runNTServer") boolean runNTServer,
            @JsonProperty("shouldManage") boolean shouldManage,
            @JsonProperty("shouldPublishProto") boolean shouldPublishProto,
            @JsonProperty("networkManagerIface") String networkManagerIface,
            @JsonProperty("setStaticCommand") String setStaticCommand,
            @JsonProperty("setDHCPcommand") String setDHCPcommand) {
        this.ntServerAddress = ntServerAddress;
        this.connectionType = connectionType;
        this.staticIp = staticIp;
        this.hostname = hostname;
        this.runNTServer = runNTServer;
        this.shouldPublishProto = shouldPublishProto;
        this.networkManagerIface = networkManagerIface;
        this.setStaticCommand = setStaticCommand;
        this.setDHCPcommand = setDHCPcommand;
        setShouldManage(shouldManage);
    }

    public NetworkConfig(NetworkConfig config) {
        this(
                config.ntServerAddress,
                config.connectionType,
                config.staticIp,
                config.hostname,
                config.runNTServer,
                config.shouldManage,
                config.shouldPublishProto,
                config.networkManagerIface,
                config.setStaticCommand,
                config.setDHCPcommand);
    }

    @JsonIgnore
    public String getPhysicalInterfaceName() {
        return this.networkManagerIface;
    }

    @JsonIgnore
    public String getEscapedInterfaceName() {
        return "\"" + networkManagerIface + "\"";
    }

    public void setShouldManage(boolean shouldManage) {
        this.shouldManage = shouldManage && this.deviceCanManageNetwork();
    }

    @JsonIgnore
    protected boolean deviceCanManageNetwork() {
        return Platform.isLinux();
    }

    @Override
    public String toString() {
        return "NetworkConfig [serverAddr="
                + ntServerAddress
                + ", connectionType="
                + connectionType
                + ", staticIp="
                + staticIp
                + ", hostname="
                + hostname
                + ", runNTServer="
                + runNTServer
                + ", networkManagerIface="
                + networkManagerIface
                + ", setStaticCommand="
                + setStaticCommand
                + ", setDHCPcommand="
                + setDHCPcommand
                + ", shouldManage="
                + shouldManage
                + "]";
    }
}

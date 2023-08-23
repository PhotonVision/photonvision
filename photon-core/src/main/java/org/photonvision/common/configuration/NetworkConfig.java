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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkMode;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.file.JacksonUtils;

public class NetworkConfig {
    // Can be a integer team number, or a IP address
    public String ntServerAddress = "0";
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String staticIp = "";
    public String hostname = "photonvision";
    public boolean runNTServer = false;
    public boolean shouldManage;

    @JsonIgnore public static final String NM_IFACE_STRING = "${interface}";
    @JsonIgnore public static final String NM_IP_STRING = "${ipaddr}";

    public String networkManagerIface;
    public String setStaticCommand =
            "nmcli con mod ${interface} ipv4.addresses ${ipaddr}/8 ipv4.method \"manual\" ipv6.method \"disabled\"";
    public String setDHCPcommand =
            "nmcli con mod ${interface} ipv4.method \"auto\" ipv6.method \"disabled\"";


    public NetworkConfig() {
        if (Platform.isLinux()) {
            // Default to the name of the first Ethernet connection. Otherwise, "Wired connection 1" is a reasonable guess
            this.networkManagerIface = NetworkUtils.getAllWiredInterfaces().stream().map(it -> it.connName).findFirst().orElse("Wired connection 1");
        }

        // We can (usually) manage networking on Linux devices, and if we can we should try to. Command line inhibitions happen at a level above this class
        setShouldManage(Platform.isLinux());
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
            @JsonProperty("networkManagerIface") String networkManagerIface,
            @JsonProperty("setStaticCommand") String setStaticCommand,
            @JsonProperty("setDHCPcommand") String setDHCPcommand) {
        this.ntServerAddress = ntServerAddress;
        this.connectionType = connectionType;
        this.staticIp = staticIp;
        this.hostname = hostname;
        this.runNTServer = runNTServer;
        this.networkManagerIface = networkManagerIface;
        this.setStaticCommand = setStaticCommand;
        this.setDHCPcommand = setDHCPcommand;
        setShouldManage(shouldManage);
    }

    public Map<String, Object> toHashMap() {
        try {
            return new ObjectMapper().convertValue(this, JacksonUtils.UIMap.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public String getPhysicalInterfaceName() {
        return NetworkUtils.getNMinfoForConnName(this.networkManagerIface).devName;
    }

    @JsonIgnore
    public String getEscapedInterfaceName() {
        return "\"" + networkManagerIface + "\"";
    }

    @JsonIgnore
    public boolean shouldManage() {
        return this.shouldManage;
    }

    @JsonIgnore
    public void setShouldManage(boolean shouldManage) {
        this.shouldManage = shouldManage;
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

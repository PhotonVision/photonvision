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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkMode;
import org.photonvision.common.util.file.JacksonUtils;

public class NetworkConfig {
    public int teamNumber = 0;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String staticIp = "";
    public String hostname = "photonvision";
    public boolean runNTServer = false;

    @JsonIgnore public static final String NM_IFACE_STRING = "${interface}";
    @JsonIgnore public static final String NM_IP_STRING = "${ipaddr}";

    public String networkManagerIface = "Wired\\ connection\\ 1";
    public String physicalInterface = "eth0";
    public String setStaticCommand =
            "nmcli con mod ${interface} ipv4.addresses ${ipaddr}/8 ipv4.method \"manual\" ipv6.method \"disabled\"";
    public String setDHCPcommand =
            "nmcli con mod ${interface} ipv4.method \"auto\" ipv6.method \"disabled\"";

    private boolean shouldManage;

    public NetworkConfig() {
        setShouldManage(false);
    }

    @JsonCreator
    public NetworkConfig(
            @JsonProperty("teamNumber") int teamNumber,
            @JsonProperty("connectionType") NetworkMode connectionType,
            @JsonProperty("staticIp") String staticIp,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("runNTServer") boolean runNTServer,
            @JsonProperty("shouldManage") boolean shouldManage,
            @JsonProperty("networkManagerIface") String networkManagerIface,
            @JsonProperty("physicalInterface") String physicalInterface,
            @JsonProperty("setStaticCommand") String setStaticCommand,
            @JsonProperty("setDHCPcommand") String setDHCPcommand) {
        this.teamNumber = teamNumber;
        this.connectionType = connectionType;
        this.staticIp = staticIp;
        this.hostname = hostname;
        this.runNTServer = runNTServer;
        this.networkManagerIface = networkManagerIface;
        this.physicalInterface = physicalInterface;
        this.setStaticCommand = setStaticCommand;
        this.setDHCPcommand = setDHCPcommand;
        setShouldManage(shouldManage);
    }

    public static NetworkConfig fromHashMap(Map<String, Object> map) {
        try {
            return new ObjectMapper().convertValue(map, NetworkConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new NetworkConfig();
        }
    }

    public Map<String, Object> toHashMap() {
        try {
            return new ObjectMapper().convertValue(this, JacksonUtils.UIMap.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @JsonGetter("shouldManage")
    public boolean shouldManage() {
        return this.shouldManage || Platform.isLinux();
    }

    @JsonSetter("shouldManage")
    public void setShouldManage(boolean shouldManage) {
        this.shouldManage = shouldManage || Platform.isLinux();
    }
}

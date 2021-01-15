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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkMode;

public class NetworkConfig {
    public int teamNumber = 0;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String staticIp = "";
    public String hostname = "photonvision";
    public boolean runNTServer = false;

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
            @JsonProperty("shouldManage") boolean shouldManage) {
        this.teamNumber = teamNumber;
        this.connectionType = connectionType;
        this.staticIp = staticIp;
        this.hostname = hostname;
        this.runNTServer = runNTServer;
        setShouldManage(shouldManage);
    }

    public static NetworkConfig fromHashMap(Map<String, Object> map) {
        // teamNumber (int), supported (bool), connectionType (int),
        // staticIp (str), netmask (str), hostname (str)
        var ret = new NetworkConfig();
        ret.teamNumber = Integer.parseInt(map.get("teamNumber").toString());
        ret.connectionType = NetworkMode.values()[(Integer) map.get("connectionType")];
        ret.staticIp = (String) map.get("staticIp");
        ret.hostname = (String) map.get("hostname");
        ret.runNTServer = (Boolean) map.get("runNTServer");
        ret.setShouldManage((Boolean) map.get("supported"));
        return ret;
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", teamNumber);
        tmp.put("supported", shouldManage());
        tmp.put("connectionType", connectionType.ordinal());
        tmp.put("staticIp", staticIp);
        tmp.put("hostname", hostname);
        tmp.put("runNTServer", runNTServer);
        return tmp;
    }

    @JsonGetter("shouldManage")
    public boolean shouldManage() {
        return this.shouldManage || Platform.isRaspberryPi();
    }

    @JsonSetter("shouldManage")
    public void setShouldManage(boolean shouldManage) {
        this.shouldManage = shouldManage || Platform.isRaspberryPi();
    }
}

/*
 * Copyright (C) 2020 Photon Vision.
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

import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.networking.NetworkMode;

public class NetworkConfig {
    public int teamNumber = 1;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String staticIp = "";
    public String netmask = "";
    public String hostname = "photonvision";

    // TODO implement networking
    public boolean shouldManage;

    public NetworkConfig() {}

    public NetworkConfig(
            int teamNumber,
            NetworkMode connectionType,
            String staticIp,
            String netmask,
            String hostname,
            boolean shouldManage) {
        this.teamNumber = teamNumber;
        this.connectionType = connectionType;
        this.staticIp = staticIp;
        this.netmask = netmask;
        this.hostname = hostname;
        this.shouldManage = shouldManage;
    }

    public static NetworkConfig fromHashMap(Map<String, Object> map) {
        // teamNumber (int), supported (bool), connectionType (int),
        // staticIp (str), netmask (str), hostname (str)
        var ret = new NetworkConfig();
        ret.teamNumber = Integer.parseInt(map.get("teamNumber").toString());
        ret.shouldManage = (Boolean) map.get("supported");
        ret.connectionType = NetworkMode.values()[(Integer) map.get("connectionType")];
        ret.staticIp = (String) map.get("staticIp");
        ret.netmask = (String) map.get("netmask");
        ret.hostname = (String) map.get("hostname");
        return ret;
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", teamNumber);
        tmp.put("supported", shouldManage);
        tmp.put("connectionType", connectionType.ordinal());
        tmp.put("staticIp", staticIp);
        tmp.put("netmask", netmask);
        tmp.put("hostname", hostname);
        return tmp;
    }
}

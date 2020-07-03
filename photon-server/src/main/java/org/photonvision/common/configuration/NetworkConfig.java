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

import org.photonvision.common.networking.NetworkMode;

import java.util.HashMap;

public class NetworkConfig {
    public int teamNumber = 1;
    public NetworkMode connectionType = NetworkMode.DHCP;
    public String ip = "";
    public String gateway = "";
    public String netmask = "";
    public String hostname = "photonvision";

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", teamNumber);
        tmp.put("connectionType", connectionType.ordinal());
        tmp.put("ip", ip);
        tmp.put("gateway", gateway);
        tmp.put("netmask", netmask);
        return tmp;
    }
}

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

package org.photonvision.common.networking;

// using a separate class because Spotless fails on text blocks
// spotless:off
public class NetworkingCommands {
    public static final String addConnectionCommand = """
        nmcli connection add
        con-name "${connection}"
        ifname "${interface}"
        type ethernet
        """.replaceAll("[\\n]", " ");

    public static final String modStaticCommand = """
        nmcli connection modify ${connection}
        autoconnect yes
        ipv4.method manual
        ipv6.method disabled
        ipv4.addresses ${ipaddr}/8
        ipv4.gateway ${gateway}
        """.replaceAll("[\\n]", " ");

    public static final String modDHCPCommand = """
        nmcli connection modify "${connection}"
        autoconnect yes
        ipv4.method auto
        ipv6.method disabled
        """.replaceAll("[\\n]", " ");
}
//spotless:on

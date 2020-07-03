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

package org.photonvision.common.networking;

import java.net.InterfaceAddress;

@SuppressWarnings("WeakerAccess")
public class NetworkInterface {
    public final String name;
    public final String displayName;
    public final String IPAddress;
    public final String Netmask;
    public final String Gateway;
    public final String Broadcast;

    public NetworkInterface(java.net.NetworkInterface inetface, InterfaceAddress ifaceAddress) {
        name = inetface.getName();
        displayName = inetface.getDisplayName();

        var inetAddress = ifaceAddress.getAddress();
        IPAddress = inetAddress.getHostAddress();
        Netmask = getIPv4LocalNetMask(ifaceAddress);

        // TODO: (low) hack to "get" gateway, this is gross and bad, pls fix
        var splitIPAddr = IPAddress.split("\\.");
        splitIPAddr[3] = "1";
        Gateway = String.join(".", splitIPAddr);
        splitIPAddr[3] = "255";
        Broadcast = String.join(".", splitIPAddr);
    }

    private static String getIPv4LocalNetMask(InterfaceAddress interfaceAddress) {
        var netPrefix = interfaceAddress.getNetworkPrefixLength();
        try {
            // Since this is for IPv4, it's 32 bits, so set the sign value of
            // the int to "negative"...
            int shiftby = (1 << 31);
            // For the number of bits of the prefix -1 (we already set the sign bit)
            for (int i = netPrefix - 1; i > 0; i--) {
                // Shift the sign right... Java makes the sign bit sticky on a shift...
                // So no need to "set it back up"...
                shiftby = (shiftby >> 1);
            }
            // Transform the resulting value in xxx.xxx.xxx.xxx format, like if
            /// it was a standard address...
            // Return the address thus created...
            return ((shiftby >> 24) & 255)
                    + "."
                    + ((shiftby >> 16) & 255)
                    + "."
                    + ((shiftby >> 8) & 255)
                    + "."
                    + (shiftby & 255);
            //            return InetAddress.getByName(maskString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Something went wrong here...
        return null;
    }
}

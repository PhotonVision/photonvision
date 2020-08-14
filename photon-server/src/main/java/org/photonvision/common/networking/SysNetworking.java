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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import org.photonvision.common.util.ShellExec;

public abstract class SysNetworking {
    NetworkInterface networkInterface;
    ShellExec shell = new ShellExec(true, true);

    private String hostname = getHostname();

    public String getHostname() {
        if (hostname == null) {
            try {
                var retCode = shell.execute("hostname", null, true);
                if (retCode == 0) {
                    while (!shell.isOutputCompleted()) {}
                    return shell.getOutput();
                } else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        } else return hostname;
    }

    // code belongs to
    // https://stackoverflow.com/questions/19531411/calculate-cidr-from-a-given-netmask-java
    public static int convertNetmaskToCIDR(InetAddress netmask) {

        byte[] netmaskBytes = netmask.getAddress();
        int cidr = 0;
        boolean zero = false;
        for (byte b : netmaskBytes) {
            int mask = 0x80;

            for (int i = 0; i < 8; i++) {
                int result = b & mask;
                if (result == 0) {
                    zero = true;
                } else if (zero) {
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public abstract boolean setDHCP();

    public abstract boolean setHostname(String hostname);

    public abstract boolean setStatic(String ipAddress, String netmask);

    public abstract List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException;
}

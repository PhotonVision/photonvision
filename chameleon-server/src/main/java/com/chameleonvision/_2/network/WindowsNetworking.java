package com.chameleonvision._2.network;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WindowsNetworking extends SysNetworking {

    @Override
    public boolean setDHCP() {
        return false;
    }

    @Override
    public boolean setHostname(String newHostname) {
        var currentHostname = getHostname();

        if (getHostname() == null) {
            return false;
        }

        String command =
                String.format(
                        "wmic computersystem where name=\"%s\" call rename name=\"%s\"",
                        currentHostname, newHostname);

        try {
            var process = Runtime.getRuntime().exec(command);
            var returnCode = process.waitFor();
            return returnCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setStatic(String ipAddress, String netmask, String gateway) {
        return false;
    }

    @Override
    public List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException {
        var netInterfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces());

        List<java.net.NetworkInterface> goodInterfaces = new ArrayList<>();

        for (var netInterface : netInterfaces) {
            if (netInterface.getDisplayName().toLowerCase().contains("bluetooth")) continue;
            if (netInterface.getDisplayName().toLowerCase().contains("virtual")) continue;
            if (netInterface.getDisplayName().toLowerCase().contains("loopback")) continue;
            if (!netInterface.isUp()) continue;
            goodInterfaces.add(netInterface);
        }
        return goodInterfaces;
    }
}

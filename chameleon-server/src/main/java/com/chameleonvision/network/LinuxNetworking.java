package com.chameleonvision.network;

import io.javalin.core.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxNetworking extends SysNetworking {

    @Override
    public boolean setDHCP() {
        File interfaces = new File("/etc/network/interfaces");
        try {
            List<String> lines = FileUtils.readLines(interfaces, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("iface " + networkInterface.name)) {
                    line = "iface " + networkInterface.name + "inet dhcp";
                    lines.set(i, line);
                    List<Integer> rLines = new ArrayList<>();
                    for (var j = i; j < lines.size(); j++) {
                        String tmp = lines.get(j);
                        if (tmp.contains("address") || tmp.contains("netmask") || tmp.contains("gateway")) {
                            rLines.add(j);
                        }
                        if (tmp.contains("iface")) {
                            break;
                        }
                    }
                    for (Integer rLine : rLines) {
                        lines.remove(rLine.intValue());
                    }
                    FileUtils.writeLines(interfaces, lines);
                    Process p = Runtime.getRuntime().exec("systemctl restart network");
                    p.waitFor();
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean setHostname(String newHostname) {
        String[] setHostnameArgs = {"set-hostname", newHostname};
        try {
            var setHostnameRetCode = shell.execute("hostnamectl", setHostnameArgs);
            return setHostnameRetCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean setStatic(String ipAddress, String netmask, String gateway, String broadcast) {
        File interfaces = new File("/etc/network/interfaces");
        try {
            List<String> lines = FileUtils.readLines(interfaces, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("iface " + networkInterface.name)) {
                    line = "iface " + networkInterface.name + "inet static";
                    lines.set(i, line);
                    lines.add(i + 1, "address " + ipAddress);
                    lines.add(i + 2, "netmask " + netmask);
                    lines.add(i + 2, "gateway " + gateway);
                    FileUtils.writeLines(interfaces, lines);
                    Process p = Runtime.getRuntime().exec("systemctl restart network");
                    p.waitFor();
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException {
        List<java.net.NetworkInterface> netInterfaces;
        try {
            netInterfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return null;
        }

        List<java.net.NetworkInterface> goodInterfaces = new ArrayList<>();

        for (var netInterface : netInterfaces) {
            if (netInterface.getDisplayName().contains("lo")) continue;
            if (!netInterface.isUp()) continue;
            goodInterfaces.add(netInterface);
        }
        return goodInterfaces;

    }
}

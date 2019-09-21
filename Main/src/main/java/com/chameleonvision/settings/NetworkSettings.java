package com.chameleonvision.settings;

import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.commons.lang3.SystemUtils;

public class NetworkSettings {
    public String connectionType, ip, netmask, gateway, hostname;

    public void run() {
        String adapter = getAdapter();
        if (SystemUtils.IS_OS_LINUX) {//TODO check linux commands
            if (!adapter.equals("")) {
                executeCommand("ifconfig " + adapter + " down");
                if (connectionType.equals("DHCP"))
                    executeCommand("dhclient -r " + adapter);
                else if (connectionType.equals("Static")) {
                    executeCommand("ifconfig " + adapter + " " + this.ip + " netmask " + this.netmask);
                    executeCommand("route add default gw " + this.gateway + " " + adapter);
                }
                executeCommand("ifconfig " + adapter + " up");
            }
            executeCommand("hostnamectl set-hostname " + this.hostname);
        }
        //TODO add windows networking commands

//            else
//            if (SystemUtils.IS_OS_WINDOWS)
//            {
//                executeCommand("cmd /c COMMAND HERE");
//        }
    }

    private void executeCommand(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            System.err.println("Error while executing command!");
            e.printStackTrace();
        }
    }

    public static String getAdapter() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                Enumeration<InetAddress> ee = netint.getInetAddresses();
                for (InetAddress addr : Collections.list(ee))
                    if (addr instanceof Inet4Address)
//                    if ((addr.getAddress()[0] & 0xFF) == 10 && (addr.getAddress()[1] & 0xFF) == SettingsManager.GeneralSettings.team_number) {
                        if ((addr.getAddress()[0] & 0xFF) == 192 && (addr.getAddress()[1] & 0xFF) == 168) {
                            System.out.println("found robot network interface at " + netint.getName() + " ip: " + addr.getHostAddress());
                            return netint.getName();
                        }
            }
        } catch (SocketException e) {
            System.err.println("Socket exception while trying to find current ip");
        }
        return "";
    }
}

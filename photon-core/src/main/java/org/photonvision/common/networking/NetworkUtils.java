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

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class NetworkUtils {
    private static final Logger logger = new Logger(NetworkUtils.class, LogGroup.General);

    public enum NMType {
        NMTYPE_ETHERNET("ethernet"),
        NMTYPE_WIFI("wifi"),
        NMTYPE_UNKNOWN("");

        NMType(String id) {
            identifier = id;
        }

        private final String identifier;

        public static NMType typeForString(String s) {
            for (var t : NMType.values()) {
                if (t.identifier.equals(s)) {
                    return t;
                }
            }
            return NMTYPE_UNKNOWN;
        }
    }

    /**
     * Contains data about network devices retrieved from "nmcli device show"
     *
     * @param connName The human-readable name used by "nmcli con"
     * @param devName The underlying device name, used by dhclient
     * @param nmType The NetworkManager device type
     */
    public static record NMDeviceInfo(String connName, String devName, NMType nmType) {
        public NMDeviceInfo(String c, String d, String type) {
            this(c, d, NMType.typeForString(type));
        }
    }

    public static boolean nmcliIsInstalled() {
        var shell = new ShellExec(true, false);
        try {
            shell.executeBashCommand(
                    "nmcli --version");

            return shell.getExitCode() == 0;
        } catch (IOException e) {
            logger.error("Could not query nmcli version", e);
            return false;
        }
    }

    private static List<NMDeviceInfo> allInterfaces = new ArrayList<>();
    private static long lastReadTimestamp = 0;

    public static List<NMDeviceInfo> getAllInterfaces() {
        long now = System.currentTimeMillis();
        if (now - lastReadTimestamp < 5000) return allInterfaces;
        else lastReadTimestamp = now;

        var ret = new ArrayList<NMDeviceInfo>();

        if (!Platform.isLinux()) {
            // Can only determine interface name on Linux, give up
            return ret;
        }

        try {
            var shell = new ShellExec(true, false);
            shell.executeBashCommand(
                    "nmcli -t -f GENERAL.CONNECTION,GENERAL.DEVICE,GENERAL.TYPE device show");
            String out = shell.getOutput();
            if (out == null) {
                return new ArrayList<>();
            }
            Pattern pattern =
                    Pattern.compile("GENERAL.CONNECTION:(.*)\nGENERAL.DEVICE:(.*)\nGENERAL.TYPE:(.*)");
            Matcher matcher = pattern.matcher(out);
            while (matcher.find()) {
                if (!matcher.group(2).equals("lo")) {
                    // only include non-loopback devices
                    ret.add(new NMDeviceInfo(matcher.group(1), matcher.group(2), matcher.group(3)));
                }
            }
        } catch (IOException e) {
            logger.error("Could not get active network interfaces!", e);
        }

        logger.debug("Found network interfaces: " + ret);

        allInterfaces = ret;
        return ret;
    }

    /**
     * Returns an immutable list of active network interfaces.
     *
     * @return The list.
     */
    public static List<NMDeviceInfo> getAllActiveInterfaces() {
        // Seems like if an interface exists but isn't actually connected, the connection name will be
        // an empty string. Check here and only return connections with non-empty names
        return getAllInterfaces().stream().filter(it -> !it.connName.trim().isEmpty()).toList();
    }

    /**
     * Returns an immutable list of all wired network interfaces.
     *
     * @return The list.
     */
    public static List<NMDeviceInfo> getAllWiredInterfaces() {
        return getAllInterfaces().stream()
                .filter(it -> it.nmType.equals(NMType.NMTYPE_ETHERNET))
                .toList();
    }

    /**
     * Returns an immutable list of all wired and active network interfaces.
     *
     * @return The list.
     */
    public static List<NMDeviceInfo> getAllActiveWiredInterfaces() {
        return getAllWiredInterfaces().stream().filter(it -> !it.connName.isBlank()).toList();
    }

    public static NMDeviceInfo getNMinfoForConnName(String connName) {
        for (NMDeviceInfo info : getAllActiveInterfaces()) {
            if (info.connName.equals(connName)) {
                return info;
            }
        }
        return null;
    }

    public static NMDeviceInfo getNMinfoForDevName(String devName) {
        for (NMDeviceInfo info : getAllActiveInterfaces()) {
            if (info.devName.equals(devName)) {
                return info;
            }
        }
        logger.warn("Could not find a match for network device " + devName);
        return null;
    }

    public static String getActiveConnection(String devName) {
        var shell = new ShellExec(true, true);
        try {
            shell.executeBashCommand(
                    "nmcli -g GENERAL.CONNECTION dev show \"" + devName + "\"", true, false);
            return shell.getOutput().strip();
        } catch (Exception e) {
            logger.error("Exception from nmcli!");
        }
        return "";
    }

    public static boolean connDoesNotExist(String connName) {
        var shell = new ShellExec(true, true);
        try {
            shell.executeBashCommand(
                    "nmcli -g GENERAL.STATE connection show \"" + connName + "\"", true, false);
            return (shell.getExitCode() == 10);
        } catch (Exception e) {
            logger.error("Exception from nmcli!");
        }
        return false;
    }

    public static String getIPAddresses(String iFaceName) {
        if (iFaceName == null || iFaceName.isBlank()) {
            return "";
        }
        List<String> addresses = new ArrayList<String>();
        try {
            var iFace = NetworkInterface.getByName(iFaceName);
            if (iFace != null && iFace.isUp()) {
                for (var addr : iFace.getInterfaceAddresses()) {
                    var addrStr = addr.getAddress().toString();
                    if (addrStr.startsWith("/")) {
                        addrStr = addrStr.substring(1);
                    }
                    addrStr = addrStr + "/" + addr.getNetworkPrefixLength();
                    addresses.add(addrStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.join(", ", addresses);
    }

    public static String getMacAddress() {
        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();
        if (config.networkManagerIface == null || config.networkManagerIface.isBlank()) {
            // This is a silly heuristic to find a network interface that PV might be using. It looks like
            // it works pretty well, but Hyper-V adapters still show up in the list. But we're using MAC
            // address as a semi-unique identifier, not as a source of truth, so this should be fine.
            // Hyper-V adapters seem to show up near the end of the list anyways, so it's super likely
            // we'll find the right adapter anyways
            try {
                for (var iface : NetworkInterface.networkInterfaces().toList()) {
                    if (iface.isUp() && !iface.isVirtual() && !iface.isLoopback()) {
                        byte[] mac = iface.getHardwareAddress();
                        if (mac == null) {
                            logger.error("No MAC address found for " + iface.getDisplayName());
                        }
                        return formatMacAddress(mac);
                    }
                }
            } catch (Exception e) {
                logger.error("Error getting MAC address:", e);
            }
            return "";
        }
        try {
            byte[] mac = NetworkInterface.getByName(config.networkManagerIface).getHardwareAddress();
            if (mac == null) {
                logger.error("No MAC address found for " + config.networkManagerIface);
                return "";
            }
            return formatMacAddress(mac);
        } catch (Exception e) {
            logger.error("Error getting MAC address for " + config.networkManagerIface, e);
            return "";
        }
    }

    private static String formatMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder(17);
        sb.append(String.format("%02X", mac[0]));
        for (int i = 1; i < mac.length; i++) {
            sb.append(String.format("-%02X", mac[i]));
        }
        return sb.toString();
    }
}

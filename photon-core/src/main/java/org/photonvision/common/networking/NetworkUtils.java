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

import edu.wpi.first.networktables.NetworkTableInstance;
import java.io.IOException;
import java.net.InetAddress;
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
            shell.executeBashCommand("nmcli --version");

            return shell.getExitCode() == 0;
        } catch (IOException e) {
            logger.error("Could not query nmcli version", e);
            return false;
        }
    }

    private static List<NMDeviceInfo> allInterfaces = null;
    private static long lastReadTimestamp = 0;
    private static long timeout = 5000; // milliseconds
    private static long retry = 500; // milliseconds

    public static synchronized List<NMDeviceInfo> getAllInterfaces() {
        var start = System.currentTimeMillis();
        if (start - lastReadTimestamp < 5000) {
            return allInterfaces;
        }
        var ret = new ArrayList<NMDeviceInfo>();

        if (Platform.isLinux()) {
            String out = null;
            try {
                var shell = new ShellExec(true, false);
                boolean networkManagerRunning = false;
                boolean tryagain = true;

                do {
                    shell.executeBashCommand(
                            "nmcli -t -f GENERAL.CONNECTION,GENERAL.DEVICE,GENERAL.TYPE device show", true, true);
                    // nmcli returns an error of 8 if NetworkManager isn't running
                    networkManagerRunning = shell.getExitCode() != 8;
                    tryagain = System.currentTimeMillis() - start < timeout;
                    if (!networkManagerRunning && tryagain) {
                        logger.debug("NetworkManager not running, retrying in " + (retry) + " milliseconds");
                        Thread.sleep(retry);
                    }
                } while (!networkManagerRunning && tryagain);

                timeout = 0; // only try once after the first time

                if (networkManagerRunning) {
                    out = shell.getOutput();
                } else {
                    logger.error(
                            "Timed out trying to reach NetworkManager, may not be able to configure networking");
                }

            } catch (IOException e) {
                logger.error("IO Exception occured when calling nmcli to get network interfaces!", e);
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for NetworkManager", e);
            }
            if (out != null) {
                Pattern pattern =
                        Pattern.compile("GENERAL.CONNECTION:(.*)\nGENERAL.DEVICE:(.*)\nGENERAL.TYPE:(.*)");
                Matcher matcher = pattern.matcher(out);
                while (matcher.find()) {
                    if (!matcher.group(2).equals("lo")) {
                        // only include non-loopback devices
                        ret.add(new NMDeviceInfo(matcher.group(1), matcher.group(2), matcher.group(3)));
                    }
                }
            }
        }
        if (!ret.equals(allInterfaces)) {
            if (ret.isEmpty()) {
                logger.error("Unable to identify network interfaces!");
            } else {
                logger.debug("Found network interfaces: " + ret);
            }
            allInterfaces = ret;
        }
        lastReadTimestamp = System.currentTimeMillis();

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

    /**
     * Gets a MAC address of a network interface. On devices where networking is managed by
     * PhotonVision, this will return the MAC address of the configured interface. Otherwise, this
     * will attempt to search for the network interface in current use and use that interface's MAC
     * address, and if that fails, it will return a MAC address from the first network interface with
     * a MAC address, as sorted by {@link NetworkInterface#networkInterfaces()}.
     *
     * @return The MAC address.
     */
    public static String getMacAddress() {
        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();
        try {
            // Not managed? See if we're connected to a network. General assumption is one interface in
            // use at a time
            if (config.networkManagerIface == null || config.networkManagerIface.isBlank()) {
                // Use NT client IP address to find the interface in use
                if (!config.runNTServer) {
                    var conn = NetworkTableInstance.getDefault().getConnections();
                    if (conn.length > 0 && !conn[0].remote_ip.equals("127.0.0.1")) {
                        var addr = InetAddress.getByName(conn[0].remote_ip);
                        return formatMacAddress(NetworkInterface.getByInetAddress(addr).getHardwareAddress());
                    }
                }
                // Connected to a localhost server or we are the server? Try resolving ourselves. Only
                // returns a localhost address when there's no other interface available on Windows, but
                // like to return a localhost address on Linux
                var localIface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                if (localIface != null) {
                    byte[] mac = localIface.getHardwareAddress();
                    if (mac != null) {
                        return formatMacAddress(mac);
                    }
                }
                // Fine. Just find something with a MAC address
                for (var iface : NetworkInterface.networkInterfaces().toList()) {
                    if (iface.isUp() && iface.getHardwareAddress() != null) {
                        return formatMacAddress(iface.getHardwareAddress());
                    }
                }
            } else { // Managed? We should have a working interface available
                var iface = NetworkInterface.getByName(config.networkManagerIface);
                if (iface != null) {
                    byte[] mac = iface.getHardwareAddress();
                    if (mac != null) {
                        return formatMacAddress(mac);
                    } else {
                        logger.error("No MAC address found for " + config.networkManagerIface);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting MAC address", e);
        }
        return "";
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

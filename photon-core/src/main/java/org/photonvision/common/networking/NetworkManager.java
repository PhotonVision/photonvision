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

import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.PlatformUtils;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkUtils.NMDeviceInfo;
import org.photonvision.common.util.ShellExec;
import org.photonvision.common.util.TimedTaskManager;

public class NetworkManager {
    private static final Logger logger = new Logger(NetworkManager.class, LogGroup.General);
    private HashMap<String, String> activeConnections = new HashMap<String, String>();

    private NetworkManager() {}

    private static class SingletonHolder {
        private static final NetworkManager INSTANCE = new NetworkManager();
    }

    public static NetworkManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private boolean isManaged = false;
    public boolean networkingIsDisabled = false; // Passed in via CLI

    public void initialize(boolean shouldManage) {
        isManaged = shouldManage && !networkingIsDisabled;
        if (!isManaged) {
            logger.info("Network management is disabled.");
            return;
        }

        if (!Platform.isLinux()) {
            logger.info("Not managing network on non-Linux platforms.");
            this.networkingIsDisabled = true;
            return;
        }

        if (!PlatformUtils.isRoot()) {
            logger.error("Cannot manage network without root!");
            this.networkingIsDisabled = true;
            return;
        }

        // Start tasks to monitor the network interface(s)
        var ethernetDevices = NetworkUtils.getAllWiredInterfaces();
        for (NMDeviceInfo deviceInfo : ethernetDevices) {
            activeConnections.put(
                    deviceInfo.devName, NetworkUtils.getActiveConnection(deviceInfo.devName));
            monitorDevice(deviceInfo.devName, 5000);
        }

        var physicalDevices = NetworkUtils.getAllActiveWiredInterfaces();
        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();
        if (physicalDevices.stream().noneMatch(it -> (it.devName.equals(config.networkManagerIface)))) {
            try {
                // if the configured interface isn't in the list of available ones, select one that is
                var iFace = physicalDevices.stream().findFirst().orElseThrow();
                logger.warn(
                        "The configured interface doesn't match any available interface. Applying configuration to "
                                + iFace.devName);
                // update NetworkConfig with found interface
                config.networkManagerIface = iFace.devName;
                ConfigManager.getInstance().requestSave();
            } catch (NoSuchElementException e) {
                // if there are no available interfaces, go with the one from settings
                logger.warn("No physical interface found. Maybe ethernet isn't connected?");
                if (config.networkManagerIface == null || config.networkManagerIface.isBlank()) {
                    // if it's also empty, there is nothing to configure
                    logger.error("No valid network interfaces to manage");
                    return;
                }
            }
        }

        logger.info(
                "Setting "
                        + config.connectionType
                        + " with team "
                        + config.ntServerAddress
                        + " on "
                        + config.networkManagerIface);

        // always set hostname (unless it's blank)
        if (!config.hostname.isBlank()) {
            setHostname(config.hostname);
        } else {
            logger.warn("Got empty hostname?");
        }

        if (config.connectionType == NetworkMode.DHCP) {
            setConnectionDHCP(config);
        } else if (config.connectionType == NetworkMode.STATIC) {
            setConnectionStatic(config);
        }
    }

    public void reinitialize() {
        initialize(ConfigManager.getInstance().getConfig().getNetworkConfig().shouldManage);

        DataChangeService.getInstance()
                .publishEvent(
                        new DataChangeEvent<Boolean>(
                                DataChangeSource.DCS_OTHER,
                                DataChangeDestination.DCD_WEBSERVER,
                                "restartServer",
                                true));
    }

    private void setHostname(String hostname) {
        try {
            var shell = new ShellExec(true, false);
            shell.executeBashCommand("cat /etc/hostname | tr -d \" \\t\\n\\r\"");
            var oldHostname = shell.getOutput().replace("\n", "");
            logger.debug("Old host name: \"" + oldHostname + "\"");
            logger.debug("New host name: \"" + hostname + "\"");

            if (!oldHostname.equals(hostname)) {
                var setHostnameRetCode =
                        shell.executeBashCommand(
                                "echo $NEW_HOSTNAME > /etc/hostname".replace("$NEW_HOSTNAME", hostname));
                setHostnameRetCode = shell.executeBashCommand("hostnamectl set-hostname " + hostname);

                // Add to /etc/hosts
                var addHostRetCode =
                        shell.executeBashCommand(
                                String.format(
                                        "sed -i \"s/127.0.1.1.*%s/127.0.1.1\\t%s/g\" /etc/hosts",
                                        oldHostname, hostname));

                shell.executeBashCommand("systemctl restart avahi-daemon.service");

                var success = setHostnameRetCode == 0 && addHostRetCode == 0;
                if (!success) {
                    logger.error(
                            "Setting hostname returned non-zero codes (hostname/hosts) "
                                    + setHostnameRetCode
                                    + "|"
                                    + addHostRetCode
                                    + "!");
                } else {
                    logger.info("Set hostname to " + hostname);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to set hostname!", e);
        }
    }

    private void setConnectionDHCP(NetworkConfig config) {
        String connName = "dhcp-" + config.networkManagerIface;

        var shell = new ShellExec();
        try {
            if (NetworkUtils.connDoesNotExist(connName)) {
                logger.info("Creating DHCP connection " + connName);
                shell.executeBashCommand(
                        NetworkingCommands.addConnectionCommand
                                .replace("${connection}", connName)
                                .replace("${interface}", config.networkManagerIface));
            }
            logger.info("Updating the DHCP connection " + connName);
            shell.executeBashCommand(
                    NetworkingCommands.modDHCPCommand.replace("${connection}", connName));
            // activate it
            logger.info("Activating DHCP connection " + connName);
            shell.executeBashCommand(
                    "nmcli connection up \"${connection}\"".replace("${connection}", connName), false);
            activeConnections.put(config.networkManagerIface, connName);
        } catch (Exception e) {
            logger.error("Exception while setting DHCP!", e);
        }
    }

    private void setConnectionStatic(NetworkConfig config) {
        String connName = "static-" + config.networkManagerIface;

        if (config.staticIp.isBlank()) {
            logger.warn("Got empty static IP?");
            return;
        }

        // guess at the gateway from the staticIp
        String[] parts = config.staticIp.split("\\.");
        parts[parts.length - 1] = "1";
        String gateway = String.join(".", parts);

        var shell = new ShellExec();
        try {
            if (NetworkUtils.connDoesNotExist(connName)) {
                // create connection
                logger.info("Creating Static connection " + connName);
                shell.executeBashCommand(
                        NetworkingCommands.addConnectionCommand
                                .replace("${connection}", connName)
                                .replace("${interface}", config.networkManagerIface));
            }
            // modify it in case the static IP address is different
            logger.info("Updating the Static connection " + connName);
            shell.executeBashCommand(
                    NetworkingCommands.modStaticCommand
                            .replace("${connection}", connName)
                            .replace("${ipaddr}", config.staticIp)
                            .replace("${gateway}", gateway));
            // activate it
            logger.info("Activating the Static connection " + connName);
            shell.executeBashCommand(
                    "nmcli connection up \"${connection}\"".replace("${connection}", connName), false);
            activeConnections.put(config.networkManagerIface, connName);
        } catch (Exception e) {
            logger.error("Error while setting static IP!", e);
        }
    }

    // Detects changes in the carrier and reinitializes after re-connect
    private void monitorDevice(String devName, int millisInterval) {
        String taskName = "deviceStatus-" + devName;
        if (TimedTaskManager.getInstance().taskActive(taskName)) {
            // task is already running
            return;
        }
        Path path = Paths.get("/sys/class/net/{device}/carrier".replace("{device}", devName));
        if (Files.notExists(path)) {
            logger.error("Can't find " + path + ", so can't monitor " + devName);
            return;
        }
        var last =
                new Object() {
                    boolean carrier = true;
                    boolean exceptionLogged = false;
                    String addresses = "";
                };
        Runnable task =
                () -> {
                    try {
                        boolean carrier = Files.readString(path).trim().equals("1");
                        if (carrier != last.carrier) {
                            if (carrier) {
                                // carrier came back
                                logger.info("Interface " + devName + " has re-connected, reinitializing");
                                reinitialize();
                            } else {
                                logger.warn("Interface " + devName + " is disconnected, check Ethernet!");
                            }
                        }
                        var iFace = NetworkInterface.getByName(devName);
                        if (iFace != null && iFace.isUp()) {
                            String tmpAddresses = "";
                            tmpAddresses = iFace.getInterfaceAddresses().toString();
                            if (!last.addresses.equals(tmpAddresses)) {
                                // addresses have changed, log the difference
                                last.addresses = tmpAddresses;
                                logger.info("Interface " + devName + " has address(es): " + last.addresses);
                            }
                            var conn = NetworkUtils.getActiveConnection(devName);
                            if (!conn.equals(activeConnections.get(devName))) {
                                logger.warn(
                                        "Unexpected connection "
                                                + conn
                                                + " active on "
                                                + devName
                                                + ". Expected "
                                                + activeConnections.get(devName));
                                logger.info("Reinitializing");
                                reinitialize();
                            }
                        }
                        last.carrier = carrier;
                        last.exceptionLogged = false;
                    } catch (Exception e) {
                        if (!last.exceptionLogged) {
                            // Log the exception only once, but keep trying
                            logger.error("Could not check network status for " + devName, e);
                            last.exceptionLogged = true;
                        }
                    }
                };

        TimedTaskManager.getInstance().addTask(taskName, task, millisInterval);
        logger.debug("Watching network interface at path: " + path);
    }
}

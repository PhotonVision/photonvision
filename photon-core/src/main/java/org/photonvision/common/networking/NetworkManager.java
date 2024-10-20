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
import org.photonvision.common.util.ShellExec;

public class NetworkManager {
    private static final Logger logger = new Logger(NetworkManager.class, LogGroup.General);

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
            return;
        }

        if (!PlatformUtils.isRoot()) {
            logger.error("Cannot manage network without root!");
            return;
        }

        var physicalDevices = NetworkUtils.getAllWiredInterfaces();

        if (physicalDevices.size() == 0) {
            logger.warn("No network interfaces available. Maybe ethernet isn't connected?");
            // start polling for an interface?
            return;
        }

        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();

        if (physicalDevices.stream().noneMatch(it -> (it.devName.equals(config.networkManagerIface)))) {
            try {
                var iFace = physicalDevices.stream().findFirst().orElseThrow();
                logger.warn("The configured interface doesn't match any available interface. Applying configuration to " + iFace.devName);
                // update NetworkConfig with actual interface
                config.networkManagerIface = iFace.devName;
                ConfigManager.getInstance().requestSave();
            } catch (Exception e) {
                // already checked that there is at least one item in physicalDevices, so this should never happen
                logger.error("No valid network interfaces to manage", e);
                return;
            }
        }

        logger.info("Setting " + config.connectionType + " with team " + config.ntServerAddress + " on " + config.networkManagerIface);

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
        initialize(ConfigManager.getInstance().getConfig().getNetworkConfig().shouldManage());

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
            logger.debug("Old host name: >" + oldHostname +"<");
            logger.debug("New host name: >" + hostname +"<");

            if (!oldHostname.equals(hostname)) {
                var setHostnameRetCode =
                        shell.executeBashCommand(
                                "echo $NEW_HOSTNAME > /etc/hostname".replace("$NEW_HOSTNAME", hostname));
                setHostnameRetCode =
                        shell.executeBashCommand("hostnamectl set-hostname " + hostname);

                // Add to /etc/hosts
                var addHostRetCode =
                        shell.executeBashCommand(
                                String.format(
                                        "sed -i \"s/127.0.1.1.*%s/127.0.1.1\\t%s/g\" /etc/hosts",
                                        oldHostname, hostname));

                if (Platform.isRaspberryPi()) {
                    // TODO: test on RaspberryPi if this is still needed
                    shell.executeBashCommand("sudo service avahi-daemon restart");
                } else {
                    shell.executeBashCommand("sudo systemctl restart avahi-daemon.service");
                }

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

        String addDHCPcommand = """
            nmcli connection add
            con-name "${connection}"
            ifname "${interface}"
            type ethernet
            autoconnect no
            ipv4.method auto
            ipv6.method disabled
            """;
        addDHCPcommand = addDHCPcommand.replaceAll("[\\n]", " ");

        var shell = new ShellExec();
        try {
            // set nmcli back to DHCP, and re-run dhclient -- this ought to grab a new IP address
            if (NetworkUtils.connDoesNotExist(connName)) {
                // create connection
                logger.info("Creating the DHCP connection " + connName );
                shell.executeBashCommand(
                    addDHCPcommand
                        .replace("${connection}", connName)
                        .replace("${interface}", config.networkManagerIface)
                    );
            }
            // activate it
            logger.info("Activating the DHCP connection " + connName );
            shell.executeBashCommand("nmcli connection up \"${connection}\"".replace("${connection}", connName), false);

            if (Platform.isRaspberryPi()) {
                shell.executeBashCommand("dhclient " + config.networkManagerIface, false);
            }
        } catch (Exception e) {
            logger.error("Exception while setting DHCP!", e);
        }
    }

    private void setConnectionStatic(NetworkConfig config) {
        String connName = "static-" + config.networkManagerIface;
        String addStaticCommand = """
            nmcli connection add
            con-name "${connection}"
            ifname "${interface}"
            type ethernet
            autoconnect no
            ipv4.addresses ${ipaddr}/8
            ipv4.gateway ${gateway}
            ipv4.method "manual"
            ipv6.method "disabled"
            """;
        addStaticCommand = addStaticCommand.replaceAll("[\\n]", " ");

        String modStaticCommand = "nmcli connection mod \"${connection}\" ipv4.addresses ${ipaddr}/8 ipv4.gateway ${gateway}";

        if (config.staticIp.isBlank()) {
            logger.warn("Got empty static IP?");
            return;
        }

        // guess at the gateway from the staticIp
        String[] parts = config.staticIp.split("\\.");
        parts[parts.length-1] = "1";
        String gateway = String.join(".", parts);

        var shell = new ShellExec();
        try {
            // set nmcli back to DHCP, and re-run dhclient -- this ought to grab a new IP address
            if (NetworkUtils.connDoesNotExist(connName)) {
                // create connection
                logger.info("Creating the Static connection " + connName );
                shell.executeBashCommand(
                    addStaticCommand
                        .replace("${connection}", connName)
                        .replace("${interface}", config.networkManagerIface)
                        .replace("${ipaddr}", config.staticIp)
                        .replace("${gateway}", gateway)
                    );
            } else {
                // modify it in case the static IP address is different
                logger.info("Modifying the Static connection " + connName );
                shell.executeBashCommand(
                    modStaticCommand
                        .replace("${connection}", connName)
                        .replace("${ipaddr}", config.staticIp)
                        .replace("${gateway}", gateway)
                    );
            }
            // activate it
            logger.info("Activating the Static connection " + connName );
            shell.executeBashCommand("nmcli connection up \"${connection}\"".replace("${connection}", connName), false);
        } catch (Exception e) {
            logger.error("Error while setting static IP!", e);
        }
    }
}

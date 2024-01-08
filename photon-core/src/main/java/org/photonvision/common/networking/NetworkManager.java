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
import org.photonvision.common.hardware.Platform;
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

        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();
        logger.info("Setting " + config.connectionType + " with team " + config.ntServerAddress);
        if (Platform.isLinux()) {
            if (!Platform.isRoot()) {
                logger.error("Cannot manage hostname without root!");
            }

            // always set hostname
            if (!config.hostname.isEmpty()) {
                try {
                    var shell = new ShellExec(true, false);
                    shell.executeBashCommand("cat /etc/hostname | tr -d \" \\t\\n\\r\"");
                    var oldHostname = shell.getOutput().replace("\n", "");

                    var setHostnameRetCode =
                            shell.executeBashCommand(
                                    "echo $NEW_HOSTNAME > /etc/hostname".replace("$NEW_HOSTNAME", config.hostname));
                    setHostnameRetCode =
                            shell.executeBashCommand("hostnamectl set-hostname " + config.hostname);

                    // Add to /etc/hosts
                    var addHostRetCode =
                            shell.executeBashCommand(
                                    String.format(
                                            "sed -i \"s/127.0.1.1.*%s/127.0.1.1\\t%s/g\" /etc/hosts",
                                            oldHostname, config.hostname));

                    shell.executeBashCommand("sudo service avahi-daemon restart");

                    var success = setHostnameRetCode == 0 && addHostRetCode == 0;
                    if (!success) {
                        logger.error(
                                "Setting hostname returned non-zero codes (hostname/hosts) "
                                        + setHostnameRetCode
                                        + "|"
                                        + addHostRetCode
                                        + "!");
                    } else {
                        logger.info("Set hostname to " + config.hostname);
                    }
                } catch (Exception e) {
                    logger.error("Failed to set hostname!", e);
                }

            } else {
                logger.warn("Got empty hostname?");
            }

            if (config.connectionType == NetworkMode.DHCP) {
                var shell = new ShellExec();
                try {
                    // set nmcli back to DHCP, and re-run dhclient -- this ought to grab a new IP address
                    shell.executeBashCommand(
                            config.setDHCPcommand.replace(
                                    NetworkConfig.NM_IFACE_STRING, config.getEscapedInterfaceName()));
                    shell.executeBashCommand("dhclient " + config.getPhysicalInterfaceName(), false);
                } catch (Exception e) {
                    logger.error("Exception while setting DHCP!");
                }
            } else if (config.connectionType == NetworkMode.STATIC) {
                var shell = new ShellExec();
                if (!config.staticIp.isEmpty()) {
                    try {
                        shell.executeBashCommand(
                                config
                                        .setStaticCommand
                                        .replace(NetworkConfig.NM_IFACE_STRING, config.getEscapedInterfaceName())
                                        .replace(NetworkConfig.NM_IP_STRING, config.staticIp));

                        if (Platform.isRaspberryPi()) {
                            // Pi's need to manually have their interface adjusted?? and the 5-second sleep is
                            // integral in my testing (Matt)
                            shell.executeBashCommand(
                                    "sh -c 'nmcli con down "
                                            + config.getEscapedInterfaceName()
                                            + "; nmcli con up "
                                            + config.getEscapedInterfaceName()
                                            + "'");
                        } else {
                            // for now just bring down /up -- more testing needed on beelink et al.
                            shell.executeBashCommand(
                                    "sh -c 'nmcli con down "
                                            + config.getEscapedInterfaceName()
                                            + "; nmcli con up "
                                            + config.getEscapedInterfaceName()
                                            + "'");
                        }
                    } catch (Exception e) {
                        logger.error("Error while setting static IP!", e);
                    }
                } else {
                    logger.warn("Got empty static IP?");
                }
            }
        } else {
            logger.info("Not managing network on non-Linux platforms");
        }
    }

    public void reinitialize() {
        initialize(ConfigManager.getInstance().getConfig().getNetworkConfig().shouldManage());
    }
}

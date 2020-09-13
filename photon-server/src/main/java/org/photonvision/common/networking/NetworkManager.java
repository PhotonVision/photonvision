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

import org.photonvision.common.configuration.ConfigManager;
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

    public void initialize(boolean shouldManage) {
        isManaged = shouldManage;
        if (!isManaged) {
            return;
        }

        var config = ConfigManager.getInstance().getConfig().getNetworkConfig();
        logger.info(
                "Setting static ip to \""
                        + config.staticIp
                        + "\" and hostname to \""
                        + config.hostname
                        + "\"");
        if (Platform.isLinux()) {
            if (!Platform.isRoot) {
                logger.error("Cannot manage network without root!");
                return;
            }

            // always set hostname
            if (config.hostname.length() > 0) {
                try {
                    var setHostnameRetCode =
                            new ShellExec().execute("hostnamectl", "set-hostname", config.hostname);
                    var success = setHostnameRetCode == 0;
                    if (!success) {
                        logger.error("hostnamectl return non-zero exit code " + setHostnameRetCode + "!");
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
                    if (config.staticIp != "") {
                        shell.executeBashCommand("ip addr del " + config.staticIp + "/8 dev eth0");
                    }
                    shell.executeBashCommand("dhclient eth0");
                } catch (Exception e) {
                    logger.error("Exception while setting DHCP!");
                }
            } else if (config.connectionType == NetworkMode.STATIC) {
                var shell = new ShellExec();
                if (config.staticIp.length() > 0) {
                    try {
                        shell.executeBashCommand("ip addr add " + config.staticIp + "/8" + " dev eth0");
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

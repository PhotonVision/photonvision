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

package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.HashMap;
import java.util.function.Consumer;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.scripting.ScriptEventType;
import org.photonvision.common.scripting.ScriptManager;
import org.photonvision.common.util.TimedTaskManager;

public class NetworkTablesManager {
    private final NetworkTableInstance ntInstance = NetworkTableInstance.getDefault();
    private final String kRootTableName = "/photonvision";
    public final NetworkTable kRootTable = ntInstance.getTable(kRootTableName);

    private boolean isRetryingConnection = false;

    private NetworkTablesManager() {
        ntInstance.addLogger(0, 255, new NTLogger()); // to hide error messages
        TimedTaskManager.getInstance().addTask("NTManager", this::ntTick, 5000);
    }

    private static NetworkTablesManager INSTANCE;

    public static NetworkTablesManager getInstance() {
        if (INSTANCE == null) INSTANCE = new NetworkTablesManager();
        return INSTANCE;
    }

    private static final Logger logger = new Logger(NetworkTablesManager.class, LogGroup.General);

    private static class NTLogger implements Consumer<NetworkTableEvent> {
        private boolean hasReportedConnectionFailure = false;
        private long lastConnectMessageMillis = 0;

        @Override
        public void accept(NetworkTableEvent event) {
            if (!hasReportedConnectionFailure && event.logMessage.message.contains("timed out")) {
                logger.error("NT Connection has failed! Will retry in background.");
                hasReportedConnectionFailure = true;
                getInstance().broadcastConnectedStatus();
            } else if (event.logMessage.message.contains("connected")
                    && System.currentTimeMillis() - lastConnectMessageMillis > 125) {
                logger.info("NT Connected!");
                hasReportedConnectionFailure = false;
                lastConnectMessageMillis = System.currentTimeMillis();
                ScriptManager.queueEvent(ScriptEventType.kNTConnected);
                getInstance().broadcastVersion();
                getInstance().broadcastConnectedStatus();
            }
        }
    }

    public void broadcastConnectedStatus() {
        TimedTaskManager.getInstance().addOneShotTask(this::broadcastConnectedStatusImpl, 1000L);
    }

    private void broadcastConnectedStatusImpl() {
        HashMap<String, Object> map = new HashMap<>();
        var subMap = new HashMap<String, Object>();

        subMap.put("connected", ntInstance.isConnected());
        if (ntInstance.isConnected()) {
            var connections = getInstance().ntInstance.getConnections();
            if (connections.length > 0) {
                subMap.put("address", connections[0].remote_ip + ":" + connections[0].remote_port);
            }
            subMap.put("clients", connections.length);
        }

        map.put("ntConnectionInfo", subMap);
        DataChangeService.getInstance()
                .publishEvent(new OutgoingUIEvent<>("networkTablesConnected", map));
    }

    private void broadcastVersion() {
        kRootTable.getEntry("version").setString(PhotonVersion.versionString);
        kRootTable.getEntry("buildDate").setString(PhotonVersion.buildDate);
    }

    public void setConfig(NetworkConfig config) {
        if (config.runNTServer) {
            setServerMode();
        } else {
            setClientMode(config.teamNumber);
        }
        broadcastVersion();
    }

    private void setClientMode(int teamNumber) {
        if (!isRetryingConnection) logger.info("Starting NT Client");
        ntInstance.stopServer();
        ntInstance.startClient4("photonvision");
        ntInstance.setServerTeam(teamNumber);
        ntInstance.startDSClient();
        broadcastVersion();
    }

    private void setServerMode() {
        logger.info("Starting NT Server");
        ntInstance.stopClient();
        ntInstance.startServer();
        broadcastVersion();
    }

    // So it seems like if Photon starts before the robot NT server does, and both aren't static IP,
    // it'll never connect. This hack works around it by restarting the client/server while the nt
    // instance
    // isn't connected, same as clicking the save button in the settings menu (or restarting the
    // service)
    private void ntTick() {
        if (!ntInstance.isConnected()
                && !ConfigManager.getInstance().getConfig().getNetworkConfig().runNTServer) {
            setConfig(ConfigManager.getInstance().getConfig().getNetworkConfig());
        }

        if (!ntInstance.isConnected() && !isRetryingConnection) {
            isRetryingConnection = true;
            logger.error(
                    "[NetworkTablesManager] Could not connect to the robot! Will retry in the background...");
        }
    }
}

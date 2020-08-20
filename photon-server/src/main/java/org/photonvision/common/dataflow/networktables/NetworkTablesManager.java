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

package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.LogMessage;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.function.Consumer;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.scripting.ScriptEventType;
import org.photonvision.common.scripting.ScriptManager;

public class NetworkTablesManager {

    private final NetworkTableInstance ntInstance = NetworkTableInstance.getDefault();
    private final String kRootTableName = "/photonvision";
    public final NetworkTable kRootTable = ntInstance.getTable(kRootTableName);

    private NetworkTablesManager() {
        ntInstance.addLogger(new NTLogger(), 0, 255); // to hide error messages
    }

    private static NetworkTablesManager INSTANCE;

    public static NetworkTablesManager getInstance() {
        if (INSTANCE == null) INSTANCE = new NetworkTablesManager();
        return INSTANCE;
    }

    private static final Logger logger = new Logger(NetworkTablesManager.class, LogGroup.General);

    public boolean isServer = false;

    private static class NTLogger implements Consumer<LogMessage> {

        private boolean hasReportedConnectionFailure = false;
        private long lastConnectMessageMillis = 0;

        @Override
        public void accept(LogMessage logMessage) {
            if (!hasReportedConnectionFailure && logMessage.message.contains("timed out")) {
                logger.error("NT Connection has failed! Will retry in background.");
                hasReportedConnectionFailure = true;
            } else if (logMessage.message.contains("connected")
                    && System.currentTimeMillis() - lastConnectMessageMillis > 125) {
                logger.info("NT Connected!");
                hasReportedConnectionFailure = false;
                lastConnectMessageMillis = System.currentTimeMillis();
                ScriptManager.queueEvent(ScriptEventType.kNTConnected);
            }
        }
    }

    public void setConfig(NetworkConfig config) {
        if (config.teamNumber > 0) {
            setClientMode(config.teamNumber);
        } else {
            setServerMode();
        }
    }

    private void setClientMode(int teamNumber) {
        isServer = false;
        logger.info("Starting NT Client");
        ntInstance.stopServer();

        ntInstance.startClientTeam(teamNumber);
        if (ntInstance.isConnected()) {
            logger.info("[NetworkTablesManager] Connected to the robot!");
        } else {
            logger.error(
                    "[NetworkTablesManager] Could not connect to the robot! Will retry in the background...");
        }
    }

    private void setServerMode() {
        isServer = true;
        logger.info("Starting NT Server");
        ntInstance.stopClient();
        ntInstance.startServer();
    }
}

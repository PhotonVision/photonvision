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

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.networktables.LogMessage;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.RoborioFinder;
import org.photonvision.common.scripting.ScriptEventType;
import org.photonvision.common.scripting.ScriptManager;
import org.photonvision.common.util.TimedTaskManager;

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

    private static class NTLogger implements Consumer<LogMessage> {
        private boolean hasReportedConnectionFailure = false;
        private long lastConnectMessageMillis = 0;

        @Override
        public void accept(LogMessage logMessage) {
            if (!hasReportedConnectionFailure && logMessage.message.contains("timed out")) {
                logger.error("NT Connection has failed! Will retry in background.");
                hasReportedConnectionFailure = true;
                getInstance().broadcastConnectedStatus();
            } else if (logMessage.message.contains("connected")
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

        // Seperate from the above so we don't hold stuff up
        System.setProperty("java.net.preferIPv4Stack", "true");
        subMap.put(
                "deviceips",
                Arrays.stream(CameraServerJNI.getNetworkInterfaces())
                        .filter(it -> !it.equals("0.0.0.0"))
                        .toArray());
        logger.info("Searching for rios");
        List<String> possibleRioList = new ArrayList<>();
        for (var ip : CameraServerJNI.getNetworkInterfaces()) {
            logger.info("Trying " + ip);
            var possibleRioAddr = getPossibleRioAddress(ip);
            if (possibleRioAddr != null) {
                logger.info("Maybe found " + ip);
                searchForHost(possibleRioList, possibleRioAddr);
            } else {
                logger.info("Didn't match RIO IP");
            }
        }
        String name =
                "roboRIO-"
                        + ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
                        + "-FRC.local";
        searchForHost(possibleRioList, name);
        name =
                "roboRIO-"
                        + ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
                        + "-FRC.lan";
        searchForHost(possibleRioList, name);
        name =
                "roboRIO-"
                        + ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
                        + "-FRC.frc-field.local";
        searchForHost(possibleRioList, name);
        var rios = RoborioFinder.getInstance().findAll();
        for (var rio : rios) {
            possibleRioList.add(rio.getHostName());
            possibleRioList.add(String.valueOf(rio.getIpv4Address()));
        }
        subMap.put("possibleRios", possibleRioList.toArray());
        DataChangeService.getInstance()
                .publishEvent(new OutgoingUIEvent<>("networkTablesConnected", map));
    }

    String getPossibleRioAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            var address = addr.getAddress();
            if (address[0] != (byte) (10 & 0xff)) return null;
            address[3] = (byte) (2 & 0xff);
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    void searchForHost(List<String> list, String hostname) {
        try {
            logger.info("Looking up " + hostname);
            InetAddress testAddr = InetAddress.getByName(hostname);
            logger.info("Pinging " + hostname);
            var canContact = testAddr.isReachable(500);
            if (canContact) {
                logger.info("Was able to connect to " + hostname);
                if (!list.contains(hostname)) list.add(hostname);
            } else {
                logger.info("Unable to reach " + hostname);
            }
        } catch (IOException ignored) {
        }
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
        logger.info("Starting NT Client");
        ntInstance.stopServer();

        ntInstance.startClientTeam(teamNumber);
        ntInstance.startDSClient();
        if (ntInstance.isConnected()) {
            logger.info("[NetworkTablesManager] Connected to the robot!");
        } else {
            logger.error(
                    "[NetworkTablesManager] Could not connect to the robot! Will retry in the background...");
        }
        broadcastVersion();
    }

    private void setServerMode() {
        logger.info("Starting NT Server");
        ntInstance.stopClient();
        ntInstance.startServer();
        broadcastVersion();
    }
}

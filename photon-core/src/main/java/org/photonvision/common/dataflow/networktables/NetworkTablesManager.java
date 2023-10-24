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

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableEvent.Kind;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringSubscriber;
import java.io.IOException;
import java.util.EnumSet;
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
import org.photonvision.common.util.file.JacksonUtils;

public class NetworkTablesManager {
  private final NetworkTableInstance ntInstance = NetworkTableInstance.getDefault();
  private final String kRootTableName = "/photonvision";
  private final String kFieldLayoutName = "apriltag_field_layout";
  public final NetworkTable kRootTable = ntInstance.getTable(kRootTableName);

  private final NTLogger m_ntLogger = new NTLogger();

  private boolean m_isRetryingConnection = false;

  private StringSubscriber m_fieldLayoutSubscriber =
      kRootTable.getStringTopic(kFieldLayoutName).subscribe("");

  private NetworkTablesManager() {
    ntInstance.addLogger(255, 255, (event) -> {}); // to hide error messages
    ntInstance.addConnectionListener(true, m_ntLogger); // to hide error messages

    ntInstance.addListener(
        m_fieldLayoutSubscriber, EnumSet.of(Kind.kValueAll), this::onFieldLayoutChanged);

    TimedTaskManager.getInstance().addTask("NTManager", this::ntTick, 5000);

    // Get the UI state in sync with the backend. NT should fire a callback when it first connects
    // to the robot
    broadcastConnectedStatus();
  }

  private static NetworkTablesManager INSTANCE;

  public static NetworkTablesManager getInstance() {
    if (INSTANCE == null) INSTANCE = new NetworkTablesManager();
    return INSTANCE;
  }

  private static final Logger logger = new Logger(NetworkTablesManager.class, LogGroup.General);

  private static class NTLogger implements Consumer<NetworkTableEvent> {
    private boolean hasReportedConnectionFailure = false;

    @Override
    public void accept(NetworkTableEvent event) {
      var isConnEvent = event.is(Kind.kConnected);
      var isDisconnEvent = event.is(Kind.kDisconnected);

      if (!hasReportedConnectionFailure && isDisconnEvent) {
        var msg =
            String.format(
                "NT lost connection to %s:%d! (NT version %d). Will retry in background.",
                event.connInfo.remote_ip,
                event.connInfo.remote_port,
                event.connInfo.protocol_version);
        logger.error(msg);

        hasReportedConnectionFailure = true;
        getInstance().broadcastConnectedStatus();
      } else if (isConnEvent && event.connInfo != null) {
        var msg =
            String.format(
                "NT connected to %s:%d! (NT version %d)",
                event.connInfo.remote_ip,
                event.connInfo.remote_port,
                event.connInfo.protocol_version);
        logger.info(msg);

        hasReportedConnectionFailure = false;
        ScriptManager.queueEvent(ScriptEventType.kNTConnected);
        getInstance().broadcastVersion();
        getInstance().broadcastConnectedStatus();
      }
    }
  }

  private void onFieldLayoutChanged(NetworkTableEvent event) {
    var atfl_json = event.valueData.value.getString();
    try {
      System.out.println("Got new field layout!");
      var atfl = JacksonUtils.deserialize(atfl_json, AprilTagFieldLayout.class);
      ConfigManager.getInstance().getConfig().setApriltagFieldLayout(atfl);
      ConfigManager.getInstance().requestSave();
      DataChangeService.getInstance()
          .publishEvent(
              new OutgoingUIEvent<>(
                  "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));
    } catch (IOException e) {
      logger.error("Error deserializing atfl!");
      logger.error(atfl_json);
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
      var connections = ntInstance.getConnections();
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
      setClientMode(config.ntServerAddress);
    }
    broadcastVersion();
  }

  private void setClientMode(String ntServerAddress) {
    ntInstance.stopServer();
    ntInstance.startClient4("photonvision");
    try {
      int t = Integer.parseInt(ntServerAddress);
      if (!m_isRetryingConnection) logger.info("Starting NT Client, server team is " + t);
      ntInstance.setServerTeam(t);
    } catch (NumberFormatException e) {
      if (!m_isRetryingConnection)
        logger.info("Starting NT Client, server IP is \"" + ntServerAddress + "\"");
      ntInstance.setServer(ntServerAddress);
    }
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

    if (!ntInstance.isConnected() && !m_isRetryingConnection) {
      m_isRetryingConnection = true;
      logger.error(
          "[NetworkTablesManager] Could not connect to the robot! Will retry in the background...");
    }
  }
}

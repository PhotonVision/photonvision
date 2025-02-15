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
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.TimeSyncClient;
import org.photonvision.jni.TimeSyncServer;

public class TimeSyncManager {
    private static final Logger logger = new Logger(TimeSyncManager.class, LogGroup.NetworkTables);

    private TimeSyncClient m_client = null;
    private TimeSyncServer m_server = null;

    private NetworkTableInstance ntInstance;
    IntegerPublisher m_offsetPub;
    IntegerPublisher m_rtt2Pub;
    IntegerPublisher m_pingsPub;
    IntegerPublisher m_pongsPub;
    IntegerPublisher m_lastPongTimePub;

    public TimeSyncManager(NetworkTable kRootTable) {
        if (!PhotonTargetingJniLoader.isWorking) {
            logger.error("PhotonTargetingJNI was not loaded! Cannot do time-sync");
        }

        this.ntInstance = kRootTable.getInstance();

        // Need this subtable to be unique per coprocessor. TODO: consider using MAC address or
        // something similar for metrics?
        var timeTable = kRootTable.getSubTable(".timesync").getSubTable(CameraServerJNI.getHostname());
        m_offsetPub = timeTable.getIntegerTopic("offset_us").publish();
        m_rtt2Pub = timeTable.getIntegerTopic("rtt2_us").publish();
        m_pingsPub = timeTable.getIntegerTopic("ping_tx_count").publish();
        m_pongsPub = timeTable.getIntegerTopic("pong_rx_count").publish();
        m_lastPongTimePub = timeTable.getIntegerTopic("pong_rx_time_us").publish();

        // default to being a client
        logger.debug("Starting TimeSyncClient on localhost (for now)");
        m_client = new TimeSyncClient("127.0.0.1", 5810, 1.0);
    }

    // Since we're spinning off tasks in a new thread, be careful and start it seperately
    public void start() {
        if (!PhotonTargetingJniLoader.isWorking) {
            logger.error("PhotonTargetingJNI was not loaded! Cannot start");
        }

        TimedTaskManager.getInstance().addTask("TimeSyncManager::tick", this::tick, 1000);
    }

    public synchronized long getOffset() {
        if (!PhotonTargetingJniLoader.isWorking) {
            return 0;
        }

        // if we're a client, return the offset to server time
        if (m_client != null) return m_client.getOffset();
        // if we're a server, our time (nt::Now) is the same as network time
        if (m_server != null) return 0;

        // ????? should never hit
        logger.error("Client and server and null?");
        return 0;
    }

    synchronized void setConfig(NetworkConfig config) {
        if (!PhotonTargetingJniLoader.isWorking) {
            return;
        }

        if (m_client == null && m_server == null) {
            throw new RuntimeException("Neither client nor server are null?");
        }

        // if not already running a server, set it up
        if (config.runNTServer && m_server == null) {
            // tear down anything old
            if (m_client != null) {
                logger.debug("Tearing down old client");
                m_client.stop();
                m_client = null;
            }

            logger.debug("Starting TimeSyncServer");
            m_server = new TimeSyncServer(5810);
            m_server.start();
        } else
        // if not already running a client, set it up
        if (m_client == null) {
            // tear down anything old
            if (m_server != null) {
                logger.debug("Tearing down old server");
                m_server.stop();
                m_server = null;
            }

            // Guess at IP -- tick will take care of changing this (may take up to 1 second)
            logger.debug("Starting TimeSyncClient on localhost (for now)");
            m_client = new TimeSyncClient("127.0.0.1", 5810, 1.0);
        }
    }

    synchronized void tick() {
        if (m_client != null) {
            var conns = ntInstance.getConnections();

            if (conns.length > 0) {
                var newServer = conns[0].remote_ip;
                if (!m_client.getServer().equals(newServer)) {
                    logger.debug("Changing TimeSyncClient server to " + newServer);
                    m_client.setServer(newServer);
                }
            }

            if (m_client != null) {
                var m = m_client.getPingMetadata();

                m_offsetPub.set(m.offset);
                m_rtt2Pub.set(m.rtt2);
                m_pingsPub.set(m.pingsSent);
                m_pongsPub.set(m.pongsReceived);
                m_lastPongTimePub.set(m.lastPongTime);
            }
        }
    }

    public synchronized long getTimeSinceLastPong() {
        if (m_client != null) {
            return m_client.getPingMetadata().timeSinceLastPong();
        } else if (m_server != null) {
            return 0;
        } else {
            // ????
            return 0;
        }
    }

    /** Restart our timesync client if NT just connected */
    public synchronized void reportNtConnected() {
        if (m_client != null) {
            // restart (in java code; we could just add a reset metrics function...)
            logger.debug(
                    "NT (re)connected -- restarting Time Sync Client at " + m_client.getServer() + ":5810");
            m_client.stop();
            m_client = new TimeSyncClient(m_client.getServer(), 5810, 1.0);
        }
    }
}

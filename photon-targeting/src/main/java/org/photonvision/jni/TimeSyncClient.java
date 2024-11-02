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

package org.photonvision.jni;

import edu.wpi.first.networktables.NetworkTablesJNI;

/**
 * Send ping-pongs to estimate server time, relative to nt::Now. The underlying implementation does
 * technically allow us to provide a different source, but all photon code assumes nt::Now is used
 */
public class TimeSyncClient {
    public static class PingMetadata {
        // offset, us
        public long offset;
        // outgoing count
        public long pingsSent;
        // incoming count
        public long pongsReceived;
        // when we last heard back from the server, uS, in local time base
        public long lastPongTime;
        // RTT2, time from ping send to pong receive at the client, uS
        public long rtt2;

        public PingMetadata(
                long offset, long pingsSent, long pongsReceived, long lastPongTime, long rtt2) {
            this.offset = offset;
            this.pingsSent = pingsSent;
            this.pongsReceived = pongsReceived;
            this.lastPongTime = lastPongTime;
            this.rtt2 = rtt2;
        }

        @Override
        public String toString() {
            return "PingMetadata [offset="
                    + offset
                    + ", pingsSent="
                    + pingsSent
                    + ", pongsReceived="
                    + pongsReceived
                    + ", lastPongTime="
                    + lastPongTime
                    + ", rtt2="
                    + rtt2
                    + "]";
        }

        /**
         * How long, in us, since we last heard back from the server
         *
         * @return Time between last pong RX and now, or Long.MAX_VALUE if we have heard zero pongs
         */
        public long timeSinceLastPong() {
            // If no pongs, it's been forever
            if (pongsReceived < 1) {
                return Long.MAX_VALUE;
            }

            return NetworkTablesJNI.now() - lastPongTime;
        }
    }

    private final Object mutex = new Object();

    private long handle;
    private String server;
    private int port;
    private double interval;

    public TimeSyncClient(String server, int port, double interval) {
        this.server = server;
        this.port = port;
        this.interval = interval;

        this.handle = TimeSyncClient.create(server, port, interval);
        TimeSyncClient.start(handle);
    }

    public void setServer(String newServer) {
        if (!server.equals(newServer)) {
            synchronized (mutex) {
                stop();
                this.handle = TimeSyncClient.create(newServer, port, interval);
                TimeSyncClient.start(handle);
                this.server = newServer;
            }
        }
    }

    public void stop() {
        synchronized (mutex) {
            if (handle > 0) {
                TimeSyncClient.stop(handle);
                handle = 0;
            }
        }
    }

    /**
     * This offset, when added to the current value of nt::now(), yields the timestamp in the timebase
     * of the TSP Server
     *
     * @return
     */
    public long getOffset() {
        synchronized (mutex) {
            if (handle > 0) {
                return TimeSyncClient.getOffset(handle);
            }

            System.err.println("TimeSyncClient: use after free?");
            return 0;
        }
    }

    /**
     * Best estimate of the current timestamp at the TSP server
     *
     * @return The current time estimate, in microseconds, at the TSP server
     */
    public long currentServerTimestamp() {
        return NetworkTablesJNI.now() + getOffset();
    }

    public PingMetadata getPingMetadata() {
        synchronized (mutex) {
            if (handle > 0) {
                return TimeSyncClient.getLatestMetadata(handle);
            }

            System.err.println("TimeSyncClient: use after free?");
            return new PingMetadata(0, 0, 0, 0, 0);
        }
    }

    public String getServer() {
        return server;
    }

    private static native long create(String serverIP, int serverPort, double pingIntervalSeconds);

    private static native void start(long handle);

    private static native void stop(long handle);

    private static native long getOffset(long handle);

    private static native PingMetadata getLatestMetadata(long handle);
}

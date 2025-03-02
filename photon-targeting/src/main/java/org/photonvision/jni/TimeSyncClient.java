/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            if (handle != 0) {
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
            if (handle != 0) {
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
            if (handle != 0) {
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

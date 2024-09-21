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

/** Send ping-pongs to estimate server time, relative to nt::now */
public class TimeSyncClient {
    public static class PingMetadata {
        long offset;
        long pingsSent;
        long pingsRecieved;
        long lastPongTime;
    }

    final long handle;

    public TimeSyncClient(String server, int port, double interval) {
        this.handle = TimeSyncClient.create(server, port, interval);
    }

    public void start() {
        TimeSyncClient.start(handle);
    }

    public void stop() {
        TimeSyncClient.stop(handle);
    }

    /**
     * This offset, when added to the current value of nt::now(), yields the timestamp in the timebase
     * of the TSP Server
     *
     * @return
     */
    public long getOffset() {
        return TimeSyncClient.getOffset(handle);
    }

    /**
     * Best estimate of the current timestamp at the TSP server
     *
     * @return
     */
    public long currentServerTimestamp() {
        return NetworkTablesJNI.now() + getOffset();
    }

    private static native long create(String serverIP, int serverPort, double pingIntervalSeconds);

    private static native void start(long handle);

    private static native void stop(long handle);

    private static native long getOffset(long handle);

    private static native PingMetadata getLatestMetadata();
}

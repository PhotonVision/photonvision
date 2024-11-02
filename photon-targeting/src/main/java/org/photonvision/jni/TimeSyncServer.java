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

public class TimeSyncServer {
    private final Object mutex = new Object();
    private long handle;

    public TimeSyncServer(int port) {
        this.handle = TimeSyncServer.create(port);
    }

    public void start() {
        synchronized (mutex) {
            if (handle > 0) {
                TimeSyncServer.start(handle);
            } else {
                System.err.println("TimeSyncServer: use after free?");
            }
        }
    }

    public void stop() {
        if (handle > 0) {
            TimeSyncServer.stop(handle);
            handle = 0;
        }
    }

    private static native long create(int port);

    private static native void start(long handle);

    private static native void stop(long handle);
}

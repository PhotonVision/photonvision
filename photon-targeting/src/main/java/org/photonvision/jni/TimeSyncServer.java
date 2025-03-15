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

public class TimeSyncServer {
    private final Object mutex = new Object();
    private long handle;

    public TimeSyncServer(int port) {
        this.handle = TimeSyncServer.create(port);
    }

    public void start() {
        synchronized (mutex) {
            if (handle != 0) {
                TimeSyncServer.start(handle);
            } else {
                System.err.println("TimeSyncServer: use after free?");
            }
        }
    }

    public void stop() {
        if (handle != 0) {
            TimeSyncServer.stop(handle);
            handle = 0;
        }
    }

    private static native long create(int port);

    private static native void start(long handle);

    private static native void stop(long handle);
}

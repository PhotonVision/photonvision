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

package org.photonvision.common.hardware.GPIO.pi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Credit to nkolban
 * https://github.com/nkolban/jpigpio/blob/master/JPigpio/src/jpigpio/SocketLock.java
 */
final class PigpioSocketLock {
    private static final int replyTimeoutMillis = 1000;

    private final String addr;
    private final int port;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public PigpioSocketLock(String addr, int port) throws IOException {
        this.addr = addr;
        this.port = port;
        reconnect();
    }

    public void reconnect() throws IOException {
        socket = new Socket(addr, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void terminate() throws IOException {
        in.close();
        in = null;

        out.flush();
        out.close();
        out = null;

        socket.close();
        socket = null;
    }

    public synchronized int sendCmd(int cmd) throws IOException {
        byte[] b = {};
        return sendCmd(cmd, 0, 0, 0, b);
    }

    public synchronized int sendCmd(int cmd, int p1) throws IOException {
        byte[] b = {};
        return sendCmd(cmd, p1, 0, 0, b);
    }

    public synchronized int sendCmd(int cmd, int p1, int p2) throws IOException {
        byte[] b = {};
        return sendCmd(cmd, p1, p2, 0, b);
    }

    public synchronized int sendCmd(int cmd, int p1, int p2, int p3) throws IOException {
        byte[] b = {};
        return sendCmd(cmd, p1, p2, p3, b);
    }

    /**
     * Send extended command to pigpiod and return result code
     *
     * @param cmd Command to send
     * @param p1 Command parameter 1
     * @param p2 Command parameter 2
     * @param p3 Command parameter 3 (usually length of extended data - see paramater ext)
     * @param ext Array of bytes containing extended data
     * @return Command result code
     * @throws IOException in case of network connection error
     */
    @SuppressWarnings("UnusedAssignment")
    public synchronized int sendCmd(int cmd, int p1, int p2, int p3, byte[] ext) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(16 + ext.length);

        bb.putInt(Integer.reverseBytes(cmd));
        bb.putInt(Integer.reverseBytes(p1));
        bb.putInt(Integer.reverseBytes(p2));
        bb.putInt(Integer.reverseBytes(p3));

        if (ext.length > 0) {
            bb.put(ext);
        }

        out.write(bb.array());
        out.flush();

        int w = replyTimeoutMillis;
        int a = in.available();

        // if by any chance there is no response from pigpiod, then wait up to
        // specified timeout
        while (w > 0 && a < 16) {
            w -= 10;
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            a = in.available();
        }

        // throw exception if response from pigpiod has not arrived yet
        if (in.available() < 16) {
            throw new IOException(
                    "Timeout: No response from pigpio daemon within " + replyTimeoutMillis + " ms.");
        }

        int resp = Integer.reverseBytes(in.readInt()); // ignore response
        resp = Integer.reverseBytes(in.readInt()); // ignore response
        resp = Integer.reverseBytes(in.readInt()); // ignore response
        resp = Integer.reverseBytes(in.readInt()); // contains error or response
        return resp;
    }

    /**
     * Read all remaining bytes coming from pigpiod
     *
     * @param data Array to store read bytes.
     * @throws IOException if unable to read from network
     */
    public void readBytes(byte[] data) throws IOException {
        in.readFully(data);
    }
}

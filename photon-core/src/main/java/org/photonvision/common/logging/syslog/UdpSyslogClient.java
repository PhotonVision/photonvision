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

package org.photonvision.common.logging.syslog;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.photonvision.common.logging.LogLevel;

 public class UdpSyslogClient implements Closeable {
    private final InetAddress address;
    private final int port;
    private final DatagramSocket socket;
    private final String appName;

    private static final int USER_FACILITY = 1;

    public UdpSyslogClient(String appName, String address, int port) throws UnknownHostException, SocketException {
        this.port = port;
        this.appName = appName;
        this.address = InetAddress.getByName(address);
        socket = new DatagramSocket();
    }

    public void sendMessage(String message, LogLevel level) {
        sendMessage(message, null, null, level);
    }

    public void sendMessage(String message, String processId, String messageId, LogLevel level) {
        try {
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    
            String syslogMessage = String.format(
                "<%d>1 %s %s %s %s %s - %s", 
                getPriority(level),
                timestamp,
                InetAddress.getLocalHost().getHostName(),
                appName,
                processId != null ? processId : "-",
                messageId != null ? messageId : "-",
                message);
    
            byte[] messageBytes = syslogMessage.getBytes(StandardCharsets.UTF_8);
    
            // Send the message using UDP
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);

            // TODO: Does this need to be concerned with thread safety?
            socket.send(packet);
        } catch (Exception e) {} // TODO: Should this log somehow? The other implementation does a simple count of sent messages.
    }

    private int getPriority(LogLevel level) {
        int severity;
        switch (level) {
            case ERROR: severity = 3; break;
            case WARN: severity = 4; break;
            case INFO: severity = 6; break;
            case DEBUG: severity = 7; break;
            case TRACE: severity = 7; break;
            default: severity = 6; break;
        }

        return USER_FACILITY * 8 + severity;
    }

    @Override
    public void close() {
        try { // TODO: Should this log somehow?, could this cause issues?
            socket.close();
        } catch (Exception e) {}
    }
}

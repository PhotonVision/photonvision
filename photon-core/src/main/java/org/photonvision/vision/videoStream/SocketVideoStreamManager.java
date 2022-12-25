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

package org.photonvision.vision.videoStream;

import io.javalin.websocket.WsContext;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Map;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class SocketVideoStreamManager {
    private static final int NO_STREAM_PORT = -1;

    private final Logger logger = new Logger(SocketVideoStreamManager.class, LogGroup.Camera);

    private Map<Integer, SocketVideoStream> streams = new Hashtable<Integer, SocketVideoStream>();
    private Map<WsContext, Integer> userSubscriptions = new Hashtable<WsContext, Integer>();

    private static class ThreadSafeSingleton {
        private static final SocketVideoStreamManager INSTANCE = new SocketVideoStreamManager();
    }

    public static SocketVideoStreamManager getInstance() {
        return ThreadSafeSingleton.INSTANCE;
    }

    private SocketVideoStreamManager() {}

    // Register a new available camera stream
    public void addStream(SocketVideoStream newStream) {
        streams.put(newStream.portID, newStream);
        logger.debug("Added new stream for port " + Integer.toString(newStream.portID));
    }

    // Remove a previously-added camera stream, and unsubscribe all users
    public void removeStream(SocketVideoStream oldStream) {
        streams.remove(oldStream.portID);
        logger.debug("Removed stream for port " + Integer.toString(oldStream.portID));
    }

    // Indicate a user would like to subscribe to a camera stream and get frames from it periodically
    public void addSubscription(WsContext user, int streamPortID) {
        var stream = streams.get(streamPortID);
        if (stream != null) {
            userSubscriptions.put(user, streamPortID);
            stream.addUser();
        } else {
            logger.error(
                    "User attempted to subscribe to non-existent port " + Integer.toString(streamPortID));
        }
    }

    // Indicate a user would like to stop receiving one camera stream
    public void removeSubscription(WsContext user) {
        var port = userSubscriptions.get(user);
        if (port != null && port != NO_STREAM_PORT) {
            var stream = streams.get(port);
            userSubscriptions.put(user, NO_STREAM_PORT);
            if (stream != null) {
                stream.removeUser();
            }
        } else {
            logger.error(
                    "User attempted to unsubscribe, but had not yet previously subscribed successfully.");
        }
    }

    // For a given user, return the jpeg bytes (or null) for the most recent frame
    public ByteBuffer getSendFrame(WsContext user) {
        var port = userSubscriptions.get(user);
        if (port != null && port != NO_STREAM_PORT) {
            var stream = streams.get(port);
            return stream.getJPEGByteBuffer();
        } else {
            return null;
        }
    }

    // Causes all streams to "re-trigger" and recieve and convert their next mjpeg frame
    // Only invoke this after all returned jpeg Strings have been used.
    public void allStreamConvertNextFrame() {
        for (SocketVideoStream stream : streams.values()) {
            stream.convertNextFrame();
        }
    }
}

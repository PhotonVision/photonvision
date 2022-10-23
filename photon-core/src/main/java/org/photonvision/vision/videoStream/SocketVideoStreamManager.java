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

import edu.wpi.first.math.Pair;
import io.javalin.websocket.WsContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class SocketVideoStreamManager {
    private final Logger logger = new Logger(SocketVideoStreamManager.class, LogGroup.Camera);

    private Map<Integer, SocketVideoStream> streams = new Hashtable<Integer, SocketVideoStream>();

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
            stream.subscribeUser(user);
        } else {
            logger.error(
                    "User attempted to subscribe to non-existent port " + Integer.toString(streamPortID));
        }
    }

    // Indicate a user would like to stop receiving one camera stream
    public void removeSubscription(WsContext user, int streamPortID) {
        var stream = streams.get(streamPortID);
        if (stream != null) {
            stream.unsubscribeUser(user);
        } else {
            logger.error(
                    "User attempted to un-subscribe to non-existent port " + Integer.toString(streamPortID));
        }
    }

    // Indicate a user no longer should get any camera streams
    public void removeAllSubscriptions(WsContext user) {
        for (SocketVideoStream stream : streams.values()) {
            stream.unsubscribeUser(user);
        }
    }

    // For a given user, return a list of ports and jpeg byte mats to transmit
    public List<Pair<Integer, String>> getSendFrames(WsContext user) {
        var retList = new ArrayList<Pair<Integer, String>>();

        for (SocketVideoStream stream : streams.values()) {
            if (stream.userIsSubscribed(user)) {
                retList.add(new Pair<Integer, String>(stream.portID, stream.getJPEGBase64EncodedStr()));
            }
        }

        return retList;
    }

    // Causes all streams to "re-trigger" and recieve and convert their next mjpeg frame
    // Only invoke this after all returned jpeg Strings have been used.
    public void allStreamConvertNextFrame() {
        for (SocketVideoStream stream : streams.values()) {
            stream.convertNextFrame();
        }
    }
}

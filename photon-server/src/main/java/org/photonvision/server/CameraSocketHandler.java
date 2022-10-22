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

package org.photonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.tuple.Pair;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipeline.PipelineType;

@SuppressWarnings("rawtypes")
public class CameraSocketHandler {
    private final Logger logger = new Logger(CameraSocketHandler.class, LogGroup.WebServer);
    private final List<WsContext> users = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

    public static class UIMap extends HashMap<String, Object> {}

    private static class ThreadSafeSingleton {
        private static final CameraSocketHandler INSTANCE = new CameraSocketHandler();
    }

    public static CameraSocketHandler getInstance() {
        return CameraSocketHandler.ThreadSafeSingleton.INSTANCE;
    }

    private CameraSocketHandler() {

    }

    public void onConnect(WsConnectContext context) {
        context.session.setIdleTimeout(Long.MAX_VALUE); // TODO: determine better value
        var insa = context.session.getRemote().getInetSocketAddress();
        var host = insa.getAddress().toString() + ":" + insa.getPort();
        logger.info("New websocket connection from " + host);
        users.add(context);
    }

    protected void onClose(WsCloseContext context) {
        var insa = context.session.getRemote().getInetSocketAddress();
        var host = insa.getAddress().toString() + ":" + insa.getPort();
        var reason = context.reason() != null ? context.reason() : "Connection closed by client";
        logger.info("Closing websocket connection from " + host + " for reason: " + reason);
        users.remove(context);

        if (users.size() == 0) {
            logger.info("All websocket connections are closed. Setting inputShouldShow to false.");
        }
    }

    @SuppressWarnings({"unchecked"})
    public void onBinaryMessage(WsBinaryMessageContext context) {
        try {
            Map<String, Object> deserializedData =
                    objectMapper.readValue(context.data(), new TypeReference<>() {});

            // Special case the current camera index
            var camIndexValue = deserializedData.get("cameraIndex");
            Integer cameraIndex = null;
            if (camIndexValue instanceof Integer) {
                cameraIndex = (Integer) camIndexValue;
                deserializedData.remove("cameraIndex");
            }

            for (Map.Entry<String, Object> entry : deserializedData.entrySet()) {
                try {
                    var entryKey = entry.getKey();
                    var entryValue = entry.getValue();
                    var socketMessageType = CameraSocketMessageType.fromEntryKey(entryKey);

                    logger.trace(
                            () ->
                                    "Got WS message: ["
                                            + socketMessageType
                                            + "] ==> ["
                                            + entryKey
                                            + "], ["
                                            + entryValue
                                            + "]");

                    if (socketMessageType == null) {
                        logger.warn("Got unknown socket message type: " + entryKey);
                        continue;
                    }

                    switch (socketMessageType) {
                        case CSMT_CHANGEPIPELINETYPE:
                            {
                                // TODO: what is this event?
                                var data = (HashMap<String, Object>) entryValue;

                                break;
                            }
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse message!", e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to deserialize message!", e);
        }
    }

    private void sendMessage(Object message, WsContext user) throws JsonProcessingException {
        ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(message));
        user.send(b);
    }

    public void broadcastMessage(Object message, WsContext userToSkip)
            throws JsonProcessingException {
        if (userToSkip == null) {
            for (WsContext user : users) {
                sendMessage(message, user);
            }
        } else {
            var skipUserPort = userToSkip.session.getRemote().getInetSocketAddress().getPort();
            for (WsContext user : users) {
                var userPort = user.session.getRemote().getInetSocketAddress().getPort();
                if (userPort != skipUserPort) {
                    sendMessage(message, user);
                }
            }
        }
    }
}

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

import io.avaje.json.JsonException;
import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.jackson.JacksonAdapter;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import photonvision.core.proto.PhotonMessage.Event;
import photonvision.core.proto.PhotonMessage.VisionModuleEvent;
import us.hebi.quickbuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.Nullable;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipeline.PipelineType;
import org.wpilib.util.Pair;

@SuppressWarnings("rawtypes")
public class DataSocketHandler {
    private final Logger logger = new Logger(DataSocketHandler.class, LogGroup.WebServer);

    // Keep track of users. Map of sessionId to WsContext. Use ConcurrentHashMap for thread safety (I'm lazy)
    private final Map<String, WsContext> users = new ConcurrentHashMap<String, WsContext>();

    private final JacksonAdapter adapter =
            JacksonAdapter.builder().jsonFactory(new MessagePackFactory()).serializeEmpty(true).build();
    private final Jsonb msgpackJsonb = Jsonb.builder().adapter(adapter).build();
    private final DataChangeService dcService = DataChangeService.getInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private final UIOutboundSubscriber uiOutboundSubscriber = new UIOutboundSubscriber(this);

    private static class ThreadSafeSingleton {
        private static final DataSocketHandler INSTANCE = new DataSocketHandler();
    }

    public static DataSocketHandler getInstance() {
        return DataSocketHandler.ThreadSafeSingleton.INSTANCE;
    }

    private DataSocketHandler() {
        dcService.addSubscribers(
                uiOutboundSubscriber,
                new UIInboundSubscriber()); // Subscribe outgoing messages to the data change service
    }

    public void onConnect(WsConnectContext context) {
        users.put(context.sessionId(), context);

        context.session.setIdleTimeout(Duration.ofMillis(5000));
        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        var host = remote.getAddress().toString() + ":" + remote.getPort();
        logger.info("New websocket connection from " + host);
        dcService.publishEvent(
                new IncomingWebSocketEvent<>(
                        DataChangeDestination.DCD_GENSETTINGS, "userConnected", context));
    }

    protected void onClose(WsCloseContext context) {
        users.remove(context.sessionId());
        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        // Remote can be null if server is being closed for restart
        if (remote != null) {
            var host = remote.getAddress().toString() + ":" + remote.getPort();
            var reason = context.reason() != null ? context.reason() : "Connection closed by client";
            logger.info("Closing websocket connection from " + host + " for reason: " + reason);
        } else {
            logger.info("Closing websockets for user " + context.sessionId());
        }
    }

    @Json
    static record WSMessage(
            @Nullable String cameraUniqueName, @Json.Unmapped Map<String, Object> properties) {}


    @SuppressWarnings({"unchecked"})
    public void onBinaryMessage(WsBinaryMessageContext context) {
        try {
            // convert to proto
            var protoMessage = VisionModuleEvent.parseFrom(context.data());
            var camUniqueName = protoMessage.getCameraUniqueName();

            // var message = msgpackJsonb.type(WSMessage.class).fromJson(context.data());

            for (Event changeEvent : protoMessage.getChanges()) {
                try {
                    if (changeEvent.hasDriverMode()) {
                        dcService.publishEvents(
                                new IncomingWebSocketEvent<>(
                                        DataChangeDestination.DCD_ACTIVEMODULE,
                                        "isDriverMode",
                                        changeEvent.getDriverMode().getIsDriverMode(),
                                        camUniqueName,
                                        context));
                    }
                    // TODO else if ....
                } catch (Exception e) {
                    logger.error("Failed to parse message!", e);
                }
            }
        } catch (IllegalStateException | JsonException | InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize message!", e);
        }
    }

    private void sendMessage(ByteBuffer b, WsContext user) {
        if (user.session.isOpen()) {
            user.send(b);
        }
    }

    public void broadcastMessage(Object message, WsContext userToSkip) throws JsonException {
        ByteBuffer b = ByteBuffer.wrap(msgpackJsonb.toJsonBytes(message));

        if (userToSkip == null) {
            for (WsContext user : users.values()) {
                sendMessage(b, user);
            }
        } else {
            var skipUserPort = ((InetSocketAddress) userToSkip.session.getRemoteAddress()).getPort();
            for (WsContext user : users.values()) {
                var userPort = ((InetSocketAddress) user.session.getRemoteAddress()).getPort();
                if (userPort != skipUserPort) {
                    sendMessage(b, user);
                }
            }
        }
    }
}

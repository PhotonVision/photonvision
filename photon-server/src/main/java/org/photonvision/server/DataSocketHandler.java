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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.NewDataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import photonvision.core.proto.PhotonMessage.PhotonDataChangeEvent;
import photonvision.core.proto.PhotonMessage.UiChangeEvent;
import us.hebi.quickbuf.InvalidProtocolBufferException;
import us.hebi.quickbuf.ProtoMessage;

@SuppressWarnings("rawtypes")
public class DataSocketHandler {
    private final Logger logger = new Logger(DataSocketHandler.class, LogGroup.WebServer);

    // Keep track of users. Map of sessionId to WsContext. Use ConcurrentHashMap for thread safety
    // (I'm lazy)
    private final Map<String, WsContext> users = new ConcurrentHashMap<String, WsContext>();

    private final JacksonAdapter adapter =
            JacksonAdapter.builder().jsonFactory(new MessagePackFactory()).serializeEmpty(true).build();
    private final Jsonb msgpackJsonb = Jsonb.builder().adapter(adapter).build();
    private final DataChangeService dcService = DataChangeService.getInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private final UIOutboundSubscriber uiOutboundSubscriber = new UIOutboundSubscriber();

    private static class ThreadSafeSingleton {
        private static final DataSocketHandler INSTANCE = new DataSocketHandler();
    }

    public static DataSocketHandler getInstance() {
        return DataSocketHandler.ThreadSafeSingleton.INSTANCE;
    }

    private DataSocketHandler() {
        // dcService.addSubscribers(
        //         uiOutboundSubscriber,
        //         new UIInboundSubscriber()); // Subscribe outgoing messages to the data change service
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
            var protoMessage = UiChangeEvent.parseFrom(context.data());

            var event = PhotonDataChangeEvent.newInstance();
            event.setOriginCtxSessionId(context.sessionId());
            event.setEvent(protoMessage);

            // Seperate if-else by topic. Maybe this can be cleaner -- match?
            if (protoMessage.hasVmChange()) {
                NewDataChangeService.INBOUND_UI_EVENTS.publish(event);
            }
            // TODO else if ...
        } catch (IllegalStateException | JsonException | InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize message!", e);
        }
    }

    public void broadcastMessage(ProtoMessage<?> message, WsContext userToSkip) throws JsonException {
        var data = ByteBuffer.wrap(message.toByteArray());

        var skipSession = userToSkip == null ? null : userToSkip.sessionId();

        for (Map.Entry<String, WsContext> entry : users.entrySet()) {
            var userSession = entry.getKey();
            var userCtx = entry.getValue();
            if (!userSession.equals(skipSession)) {
                if (userCtx.session.isOpen()) {
                    userCtx.send(data);
                }
            }
        }
    }
}

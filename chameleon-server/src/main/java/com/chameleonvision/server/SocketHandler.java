package com.chameleonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class SocketHandler {
    static List<WsContext> users = new ArrayList<>();
    static ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

    public static void onConnect(WsConnectContext context) {
        users.add(context);
    }

    static void onClose(WsCloseContext context) {
        users.remove(context);
    }

    public static void onBinaryMessage(WsBinaryMessageContext context) {
        try {
            Map<String, Object> data =
                    objectMapper.readValue(context.data(), new TypeReference<Map<String, Object>>() {});
            // TODO pass data to ui data provider
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(Object message, WsContext user) throws JsonProcessingException {
        ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(message));
        user.send(b);
    }

    public static void broadcastMessage(Object message, WsContext userToSkip)
            throws JsonProcessingException {
        for (WsContext user : users) {
            if (user != userToSkip) {
                sendMessage(message, user);
            }
        }
    }

    public static void broadcastMessage(Object message) throws JsonProcessingException {
        broadcastMessage(message, null);
    }
}

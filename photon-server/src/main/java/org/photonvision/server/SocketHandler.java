/*
 * Copyright (C) 2020 Photon Vision.
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
            return;
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
        return;
    }

    public static void broadcastMessage(Object message) throws JsonProcessingException {
        broadcastMessage(message, null);
    }
}

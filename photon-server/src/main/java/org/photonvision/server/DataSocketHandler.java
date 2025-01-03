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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
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
public class DataSocketHandler {
    private final Logger logger = new Logger(DataSocketHandler.class, LogGroup.WebServer);
    private final List<WsContext> users = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
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
        users.add(context);
        context.session.setIdleTimeout(
                Duration.ofMillis(Long.MAX_VALUE)); // TODO: determine better value
        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        var host = remote.getAddress().toString() + ":" + remote.getPort();
        logger.info("New websocket connection from " + host);
        dcService.publishEvent(
                new IncomingWebSocketEvent<>(
                        DataChangeDestination.DCD_GENSETTINGS, "userConnected", context));
    }

    protected void onClose(WsCloseContext context) {
        users.remove(context);
        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        // Remote can be null if server is being closed for restart
        if (remote != null) {
            var host = remote.getAddress().toString() + ":" + remote.getPort();
            var reason = context.reason() != null ? context.reason() : "Connection closed by client";
            logger.info("Closing websocket connection from " + host + " for reason: " + reason);
        } else {
            logger.info("Closing websockets for user " + context.getSessionId());
        }
    }

    @SuppressWarnings({"unchecked"})
    public void onBinaryMessage(WsBinaryMessageContext context) {
        try {
            Map<String, Object> deserializedData =
                    objectMapper.readValue(context.data(), new TypeReference<>() {});

            // Special case the current camera index
            String cameraUniqueName = "";
            if (deserializedData.get("cameraUniqueName") instanceof String camUniqueNameValue) {
                cameraUniqueName = camUniqueNameValue;
                deserializedData.remove("cameraUniqueName");
            }

            for (Map.Entry<String, Object> entry : deserializedData.entrySet()) {
                try {
                    var entryKey = entry.getKey();
                    var entryValue = entry.getValue();
                    var socketMessageType = DataSocketMessageType.fromEntryKey(entryKey);

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
                        case SMT_DRIVERMODE -> {
                            // TODO: what is this event?
                            var data = (Boolean) entryValue;
                            var dmIsDriverEvent =
                                    new IncomingWebSocketEvent<Boolean>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "isDriverMode",
                                            data,
                                            cameraUniqueName,
                                            context);

                            dcService.publishEvents(dmIsDriverEvent);
                        }
                        case SMT_CHANGECAMERANAME -> {
                            var ccnEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "cameraNickname",
                                            (String) entryValue,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(ccnEvent);
                        }
                        case SMT_CHANGEPIPELINENAME -> {
                            var cpnEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "pipelineName",
                                            (String) entryValue,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(cpnEvent);
                        }
                        case SMT_ADDNEWPIPELINE -> {
                            // HashMap<String, Object> data = (HashMap<String, Object>) entryValue;
                            // var type = (PipelineType) data.get("pipelineType");
                            // var name = (String) data.get("pipelineName");
                            var arr = (ArrayList<Object>) entryValue;
                            var name = (String) arr.get(0);
                            var type = PipelineType.values()[(Integer) arr.get(1) + 2];

                            var newPipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "newPipelineInfo",
                                            Pair.of(name, type),
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(newPipelineEvent);
                        }
                        case SMT_CHANGEBRIGHTNESS -> {
                            HardwareManager.getInstance()
                                    .setBrightnessPercent(Integer.parseInt(entryValue.toString()));
                        }
                        case SMT_DUPLICATEPIPELINE -> {
                            var pipeIndex = (Integer) entryValue;

                            logger.info("Duplicating pipe@index" + pipeIndex + " for camera " + cameraUniqueName);

                            var newPipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "duplicatePipeline",
                                            pipeIndex,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(newPipelineEvent);
                        }
                        case SMT_DELETECURRENTPIPELINE -> {
                            var deleteCurrentPipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "deleteCurrPipeline",
                                            0,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(deleteCurrentPipelineEvent);
                        }
                        case SMT_ROBOTOFFSETPOINT -> {
                            var robotOffsetPointEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "robotOffsetPoint",
                                            (Integer) entryValue,
                                            cameraUniqueName,
                                            null);
                            dcService.publishEvent(robotOffsetPointEvent);
                        }
                        case SMT_CURRENTCAMERA -> {
                            var changeCurrentCameraEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_OTHER, "changeUICamera", (Integer) entryValue);
                            dcService.publishEvent(changeCurrentCameraEvent);
                        }
                        case SMT_CURRENTPIPELINE -> {
                            var changePipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "changePipeline",
                                            (Integer) entryValue,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(changePipelineEvent);
                        }
                        case SMT_STARTPNPCALIBRATION -> {
                            var changePipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "startCalibration",
                                            (Map) entryValue,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(changePipelineEvent);
                        }
                        case SMT_SAVEINPUTSNAPSHOT -> {
                            var takeInputSnapshotEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "saveInputSnapshot",
                                            0,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(takeInputSnapshotEvent);
                        }
                        case SMT_SAVEOUTPUTSNAPSHOT -> {
                            var takeOutputSnapshotEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "saveOutputSnapshot",
                                            0,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(takeOutputSnapshotEvent);
                        }
                        case SMT_TAKECALIBRATIONSNAPSHOT -> {
                            var takeCalSnapshotEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "takeCalSnapshot",
                                            0,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(takeCalSnapshotEvent);
                        }
                        case SMT_PIPELINESETTINGCHANGE -> {
                            HashMap<String, Object> data = (HashMap<String, Object>) entryValue;

                            if (data.size() >= 2) {
                                var cameraIndex2 = (String) data.get("cameraUniqueName");
                                for (var dataEntry : data.entrySet()) {
                                    if (dataEntry.getKey().equals("cameraUniqueName")) {
                                        continue;
                                    }
                                    var pipelineSettingChangeEvent =
                                            new IncomingWebSocketEvent(
                                                    DataChangeDestination.DCD_ACTIVEPIPELINESETTINGS,
                                                    dataEntry.getKey(),
                                                    dataEntry.getValue(),
                                                    cameraIndex2,
                                                    context);
                                    dcService.publishEvent(pipelineSettingChangeEvent);
                                }
                            } else {
                                logger.warn("Unknown message for PSC: " + data.keySet().iterator().next());
                            }
                        }
                        case SMT_CHANGEPIPELINETYPE -> {
                            var changePipelineEvent =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE,
                                            "changePipelineType",
                                            (Integer) entryValue,
                                            cameraUniqueName,
                                            context);
                            dcService.publishEvent(changePipelineEvent);
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

    private void sendMessage(ByteBuffer b, WsContext user) throws JsonProcessingException {
        if (user.session.isOpen()) {
            user.send(b);
        }
    }

    public void broadcastMessage(Object message, WsContext userToSkip)
            throws JsonProcessingException {
        ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(message));

        if (userToSkip == null) {
            for (WsContext user : users) {
                sendMessage(b, user);
            }
        } else {
            var skipUserPort = ((InetSocketAddress) userToSkip.session.getRemoteAddress()).getPort();
            for (WsContext user : users) {
                var userPort = ((InetSocketAddress) user.session.getRemoteAddress()).getPort();
                if (userPort != skipUserPort) {
                    sendMessage(b, user);
                }
            }
        }
    }
}

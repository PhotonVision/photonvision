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

import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.camera.IncomingCameraCommandSubscriber;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipeline.PipelineType;
import org.photonvision.vision.processes.PipelineManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.vision.processes.VisionModuleManager;

@SuppressWarnings("rawtypes")
public class SocketHandler {

    private final Logger logger = new Logger(SocketHandler.class, LogGroup.Server);
    private final List<WsContext> users = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
    private final DataChangeService dcService = DataChangeService.getInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private final UIOutboundSubscriber uiOutboundSubscriber
        = new UIOutboundSubscriber(this);
    private final IncomingCameraCommandSubscriber cameraChangeSubscriber
        = new IncomingCameraCommandSubscriber(VisionModuleManager.getInstance());

    public static class UIMap extends HashMap<String, Object> {
    }

    abstract static class SelectiveBroadcastPair extends Pair<UIMap, WsContext> {
    }

    private static class ThreadSafeSingleton {
        private static final SocketHandler INSTANCE = new SocketHandler();
    }

    public static SocketHandler getInstance() {
        return SocketHandler.ThreadSafeSingleton.INSTANCE;
    }

    private SocketHandler() {
        dcService.addSubscribers(uiOutboundSubscriber, cameraChangeSubscriber, new UIInboundSubscriber()); // Subscribe outgoing messages to the data change service
    }

    public void onConnect(WsConnectContext context) {
        users.add(context);
        dcService.publishEvent(new IncomingWebSocketEvent<>(DataChangeDestination.DCD_GENSETTINGS,
            "userConnected", context));
    }

    protected void onClose(WsCloseContext context) {
        logger.info("Closing for reason:\n    " + context.reason());
        users.remove(context);
    }

    @SuppressWarnings({"unchecked"})
    public void onBinaryMessage(WsBinaryMessageContext context) {
        try {
            Map<String, Object> deserializedData =
                objectMapper.readValue(context.data(), new TypeReference<>() {
                });

            for (Map.Entry<String, Object> entry : deserializedData.entrySet()) {
                try {
                    var entryKey = entry.getKey();
                    var entryValue = entry.getValue();
                    var socketMessageType = SocketMessageType.fromEntryKey(entryKey);

                    if (socketMessageType == null) {
                        logger.warn("Got unknown socket message type: " + entryKey);
                        continue;
                    }

                    switch (socketMessageType) {
                        case SMT_DRIVERMODE: {
                            var data = (HashMap<String, Object>) entryValue;
                            var dmExpEvent =
                                new IncomingWebSocketEvent<Integer>(
                                    DataChangeDestination.DCD_ACTIVEMODULE, "driverExposure", data);
                            var dmBrightEvent =
                                new IncomingWebSocketEvent<Integer>(
                                    DataChangeDestination.DCD_ACTIVEMODULE, "driverBrightness", data);
                            var dmIsDriverEvent =
                                new IncomingWebSocketEvent<Boolean>(
                                    DataChangeDestination.DCD_ACTIVEMODULE, "isDriver", data);

                            dcService.publishEvents(dmExpEvent, dmBrightEvent, dmIsDriverEvent);
                            break;
                        }
                        case SMT_CHANGECAMERANAME: {
                            var ccnEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE,
                                    "cameraNickname",
                                    (String) entryValue);
                            dcService.publishEvent(ccnEvent);
                            break;
                        }
                        case SMT_CHANGEPIPELINENAME: {
                            var cpnEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE,
                                    "pipelineName",
                                    (String) entryValue);
                            dcService.publishEvent(cpnEvent);
                            break;
                        }
                        case SMT_ADDNEWPIPELINE: {
                            HashMap<String, Object> data = (HashMap<String, Object>) entryValue;
                            var type = (PipelineType) data.get("pipelineType");
                            var name = (String) data.get("pipelineName");

                            var newPipelineEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE,
                                    "newPipelineInfo",
                                    Pair.of(name, type));
                            dcService.publishEvent(newPipelineEvent);
                            break;
                        }
                        case SMT_COMMAND: {
                            var cmd = SocketMessageCommandType.fromEntryKey((String) entryValue);
                            var foo = 1;
                            switch (cmd) {
                                case SMCT_DELETECURRENTPIPELINE: {
                                    var deleteCurrentPipelineEvent =
                                        new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_ACTIVEMODULE, "deleteCurrPipeline", 0);
                                    dcService.publishEvent(deleteCurrentPipelineEvent);
                                    break;
                                }
                                case SMCT_SAVE: {
                                    var saveEvent =
                                        new IncomingWebSocketEvent<>(DataChangeDestination.DCD_OTHER, "save", 0);
                                    dcService.publishEvent(saveEvent);
                                    break;
                                }
                            }
                            break;
                        }
                        case SMT_CURRENTCAMERA: {
                            var changeCurrentCameraEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_OTHER, "changeUICamera", (Integer) entryValue);
                            dcService.publishEvent(changeCurrentCameraEvent);
                            break;
                        }
                        case SMT_CURRENTPIPELINE: {
                            var changePipelineEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE,
                                    "changePipeline",
                                    (Integer) entryValue);
                            dcService.publishEvent(changePipelineEvent);
                            break;
                        }
                        case SMT_ISPNPCALIBRATION: {
                            var changePipelineEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE,
                                    "changePipeline",
                                    PipelineManager.CAL_3D_INDEX);
                            dcService.publishEvent(changePipelineEvent);
                            break;
                        }
                        case SMT_TAKECALIBRATIONSNAPSHOT: {
                            var takeCalSnapshotEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEMODULE, "takeCalSnapshot", 0);
                            dcService.publishEvent(takeCalSnapshotEvent);
                            break;
                        }
                        case SMT_PIPELINESETTINGCHANGE: {
                            HashMap<String, Object> data = (HashMap<String, Object>) entryValue;

                            // there shall only be one.
                            Map.Entry<String, Object> thisEntry = data.entrySet().iterator().next();

                            var pipelineSettingChangeEvent =
                                new IncomingWebSocketEvent<>(
                                    DataChangeDestination.DCD_ACTIVEPIPELINESETTINGS,
                                    thisEntry.getKey(),
                                    thisEntry.getValue(),
                                    (Integer) data.get("cameraIndex"));

                            dcService.publishEvent(pipelineSettingChangeEvent);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    logger.error("unknown booboo");
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            // TODO: log
            e.printStackTrace();
        }
    }

    // TODO: change to use the DataFlow system
    private void sendMessage(Object message, WsContext user) throws JsonProcessingException {
        ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(message));
        user.send(b);
    }

    // TODO: change to use the DataFlow system
    void broadcastMessage(Object message, WsContext userToSkip)
        throws JsonProcessingException {
        for (WsContext user : users) {
            if (user != userToSkip) {
                sendMessage(message, user);
            }
        }
    }
}

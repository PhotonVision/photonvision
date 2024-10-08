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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.tuple.Pair;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.target.RobotOffsetPointOperation;

@SuppressWarnings("rawtypes")
public class DataSocketHandler {
    private final Logger logger = new Logger(DataSocketHandler.class, LogGroup.WebServer);
    private final List<WsContext> users = new CopyOnWriteArrayList<>();
    private final ObjectMapper messagePackDecoder = new ObjectMapper(new MessagePackFactory());
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
                        DataChangeDestination.DCD_PROGRAM, "userConnected", null, null, context));
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
                    messagePackDecoder.readValue(context.data(), new TypeReference<>() {});

            for (Map.Entry<String, Object> entry : deserializedData.entrySet()) {
                try {
                    switch (entry.getKey()) {
                        case "changeActivePipeline" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<Integer>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "changeActivePipeline",
                                            payload.get("newActivePipelineIndex"),
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "driverMode" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<Boolean>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "driverMode",
                                            (Boolean) payload.get("driverMode"),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "changeCameraNickname" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<String>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "changeCameraNickname",
                                            (String) payload.get("nickname"),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "changePipelineNickname" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            // pipelineIndex, nickname
                            var event =
                                    new IncomingWebSocketEvent<HashMap<String, Object>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "changePipelineNickname",
                                            payload,
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "createNewPipeline" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<HashMap<String, Object>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "createNewPipeline",
                                            payload,
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "duplicatePipeline" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<HashMap<String, Object>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "duplicatePipeline",
                                            payload,
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "resetPipeline" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            // pipelineIndex, type?
                            var event =
                                    new IncomingWebSocketEvent<HashMap<String, Object>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "resetPipeline",
                                            payload,
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "deletePipeline" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<Integer>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "deletePipeline",
                                            payload.get("pipelineIndex"),
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "changePipelineSettings" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var cameraIndex = (Integer) payload.get("cameraIndex");
                            var pipelineIndex = (Integer) payload.get("pipelineIndex");

                            var configuredSettings = (Map<String, Object>) payload.get("configuredSettings");
                            for (var setting : configuredSettings.entrySet()) {
                                var pipelineSettingChangeEvent =
                                        new IncomingWebSocketEvent<Pair<Integer, Map.Entry<String, Object>>>(
                                                DataChangeDestination.DCD_VISIONMODULE,
                                                "pipelineSettingChange",
                                                Pair.of(pipelineIndex, setting),
                                                cameraIndex,
                                                context);
                                dcService.publishEvent(pipelineSettingChangeEvent);
                            }
                        }
                        case "startCalib" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            //                            videoModeIndex: number;
                            //                            squareSizeIn: number;
                            //                            markerSizeIn: number;
                            //                            patternWidth: number;
                            //                            patternHeight: number;
                            //                            boardType: CalibrationBoardTypes;
                            //                            useMrCal: boolean;
                            //                            useOldPattern: boolean;
                            //                            tagFamily: CalibrationTagFamilies;

                            var event =
                                    new IncomingWebSocketEvent<HashMap<String, Object>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "startCalib",
                                            payload,
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "takeCalibSnapshot" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "takeCalibSnapshot",
                                            null,
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "cancelCalib" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "cancelCalib",
                                            null,
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "completeCalib" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "completeCalib",
                                            null,
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "importCalibFromData" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<CameraCalibrationCoefficients>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "importCalibFromData",
                                            messagePackDecoder.convertValue(
                                                    payload.get("calibration"), CameraCalibrationCoefficients.class),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "importCalibFromCalibDB" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<CameraCalibrationCoefficients>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "importCalibFromData",
                                            CameraCalibrationCoefficients.parseFromCalibdbJson(
                                                    messagePackDecoder.readTree((String) payload.get("calibration"))),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "saveInputSnapshot" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "saveInputSnapshot",
                                            null,
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "saveOutputSnapshot" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "saveOutputSnapshot",
                                            null,
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "ledPercentage" -> {
                            var event =
                                    new IncomingWebSocketEvent<Double>(
                                            DataChangeDestination.DCD_PROGRAM,
                                            "ledPercentage",
                                            (Double) entry.getValue(),
                                            null,
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "robotOffsetPoint" -> {
                            var payload = (HashMap<String, Integer>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<RobotOffsetPointOperation>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "robotOffsetPoint",
                                            RobotOffsetPointOperation.fromIndex(payload.get("type")),
                                            payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "changeCameraFOV" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<Double>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "changeCameraFOV",
                                            (Double) payload.get("fov"),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "changeCameraQuirks" -> {
                            var payload = (HashMap<String, Object>) entry.getValue();

                            var event =
                                    new IncomingWebSocketEvent<HashMap<CameraQuirk, Boolean>>(
                                            DataChangeDestination.DCD_VISIONMODULE,
                                            "changeCameraQuirks",
                                            (HashMap<CameraQuirk, Boolean>) payload.get("quirks"),
                                            (Integer) payload.get("cameraIndex"),
                                            context);
                            dcService.publishEvent(event);
                        }
                        case "restartProgram" -> {
                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_PROGRAM, "restartProgram", null, null, context);
                            dcService.publishEvent(event);
                        }
                        case "restartDevice" -> {
                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_PROGRAM, "restartDevice", null, null, context);
                            dcService.publishEvent(event);
                        }
                        case "publishMetrics" -> {
                            var event =
                                    new IncomingWebSocketEvent<>(
                                            DataChangeDestination.DCD_PROGRAM, "publishMetrics", null, null, context);
                            dcService.publishEvent(event);
                        }
                        default -> logger.warn("Got unknown socket message request: " + entry.getKey());
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse message!", e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to deserialize incoming binary message!", e);
        }
    }

    private void sendMessage(ByteBuffer b, WsContext user) {
        if (user.session.isOpen()) {
            user.send(b);
        }
    }

    public void broadcastMessage(Object message, WsContext userToSkip)
            throws JsonProcessingException {
        ByteBuffer b = ByteBuffer.wrap(messagePackDecoder.writeValueAsBytes(message));

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

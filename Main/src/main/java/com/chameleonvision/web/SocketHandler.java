package com.chameleonvision.web;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.enums.StreamDivisor;
import com.chameleonvision.vision.pipeline.CVPipeline;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.networktables.NetworkTable;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SocketHandler {

    private static List<WsContext> users;
    private static ObjectMapper objectMapper;

    SocketHandler() {
        users = new ArrayList<>();
        objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    void onConnect(WsConnectContext context) {
        users.add(context);
        sendFullSettings();
    }

    void onClose(WsCloseContext context) {
        users.remove(context);
    }

    @SuppressWarnings("unchecked")
    void onBinaryMessage(WsBinaryMessageContext context) throws Exception {
        Map<String, Object> deserialized = objectMapper.readValue(ArrayUtils.toPrimitive(context.data()), new TypeReference<>() {
        });
        for (Map.Entry<String, Object> entry : deserialized.entrySet()) {
            try {
                VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
                CameraCapture currentCamera = currentProcess.getCamera();
                CVPipeline currentPipeline = currentProcess.pipelineManager.getCurrentPipeline();

                switch (entry.getKey()) {
                    case "driverMode": {
                        HashMap<String, Object> data = (HashMap<String, Object>) entry.getValue();
                        currentProcess.getDriverModeSettings().exposure = (Integer) data.get("driverExposure");
                        currentProcess.getDriverModeSettings().brightness = (Integer) data.get("driverBrightness");
                        currentProcess.setDriverMode((Boolean) data.get("isDriver"));

                        VisionManager.saveCurrentCameraDriverMode();
                        break;
                    }
                    case "changeCameraName": {
                        currentCamera.getProperties().setNickname((String) entry.getValue());
                        currentProcess.setCameraName((String) entry.getValue());
                        sendFullSettings();
                        VisionManager.saveCurrentCameraSettings();
                        break;
                    }
                    case "changePipelineName": {
                        currentProcess.pipelineManager.renameCurrentPipeline((String) entry.getValue());
                        sendFullSettings();
                        VisionManager.saveCurrentCameraPipelines();
                        break;
                    }
                    case "duplicatePipeline": {
                        HashMap pipelineVals = (HashMap) entry.getValue();
                        int pipelineIndex = (int) pipelineVals.get("pipeline");
                        int cameraIndex = (int) pipelineVals.get("camera");
                        ObjectMapper mapper = new ObjectMapper();
                        CVPipelineSettings origPipeline = currentProcess.pipelineManager.getPipeline(pipelineIndex).settings;
                        String val = mapper.writeValueAsString(origPipeline);
                        CVPipelineSettings newPipeline = mapper.readValue(val, origPipeline.getClass());

                        // TODO: move to PipelineManager
                        newPipeline.nickname += "(Copy)";

                        if (cameraIndex != -1) {
                            VisionProcess newProcess = VisionManager.getVisionProcessByIndex(cameraIndex);
                            if (newProcess != null) {
                                currentProcess.pipelineManager.duplicatePipeline(newPipeline, newProcess);
                            } else {
                                System.err.println("Failed to get new camera for pipeline duplication!");
                            }
                        } else {
                            currentProcess.pipelineManager.duplicatePipeline(newPipeline);
                        }

                        VisionManager.saveCurrentCameraPipelines();
                        sendFullSettings();
                        break;
                    }
                    case "command": {
                        switch ((String) entry.getValue()) {
                            case "addNewPipeline":
                                // TODO: add to UI selection for new 2d/3d
                                boolean is3d = false;
                                currentProcess.pipelineManager.addNewPipeline(is3d);
                                sendFullSettings();
                                VisionManager.saveCurrentCameraPipelines();
                                break;
                            case "deleteCurrentPipeline":
                                currentProcess.pipelineManager.deleteCurrentPipeline();
                                sendFullSettings();
                                VisionManager.saveCurrentCameraPipelines();
                                break;
                            case "save":
                                ConfigManager.saveGeneralSettings();
                                VisionManager.saveAllCameras();
                                System.out.println("Saved Settings");
                                break;
                        }
                        // used to define all incoming commands
                        break;
                    }
                    case "currentCamera": {
                        VisionManager.setCurrentProcessByIndex((Integer) entry.getValue());
                        sendFullSettings();
                        break;
                    }
                    case "currentPipeline": {
                        currentProcess.pipelineManager.setCurrentPipeline((Integer) entry.getValue());
                        sendFullSettings();
                        break;
                    }
                    default: {

                        // only change settings when we aren't in driver mode
                        if(currentProcess.pipelineManager.getDriverMode()) break;

                        setField(currentPipeline.settings, entry.getKey(), entry.getValue());
                        switch (entry.getKey()) {
                            case "exposure": {
                                currentCamera.setExposure((Integer) entry.getValue());
                                break;
                            }
                            case "brightness": {
                                currentCamera.setBrightness((Integer) entry.getValue());
                                break;
                            }
                            case "videoMode":{
                                currentCamera.setVideoMode((Integer) entry.getValue());
                                break;
                            }
                            case "streamDivisor":{
                                VisionProcess currentVisionProcess = VisionManager.getCurrentUIVisionProcess();
                                currentVisionProcess.cameraStreamer.setDivisor(StreamDivisor.values()[(Integer) entry.getValue()], true);
                                break;
                            }
                        }

                        VisionManager.saveCurrentCameraPipelines();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            broadcastMessage(deserialized, context);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getField(fieldName);
            if (field.getType().isEnum())
                field.set(obj, field.getType().getEnumConstants()[(Integer) value]);
            else
                field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    private static void broadcastMessage(Object obj, WsContext userToSkip) {
        if (users != null)
            for (WsContext user : users) {
                if (userToSkip != null && user.getSessionId().equals(userToSkip.getSessionId())) {
                    continue;
                }
                try {
                    ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(obj));
                    user.send(b);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
    }

    public static void broadcastMessage(Object obj) {
        broadcastMessage(obj, null);//Broadcasts the message to every user
    }

    private static HashMap<String, Object> getOrdinalPipeline(Class cvClass) throws IllegalAccessException {
        HashMap<String, Object> tmp = new HashMap<>();
        for (Field field : cvClass.getFields()) { // iterate over every field in CVPipelineSettings
            try {
                if (!field.getType().isEnum()) { // if the field is not an enum, get it based on the current pipeline
                    tmp.put(field.getName(), field.get(VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipeline().settings));
                } else {
                    var ordinal = (Enum) field.get(VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipeline().settings);
                    tmp.put(field.getName(), ordinal.ordinal());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", ConfigManager.settings.teamNumber);
        tmp.put("connectionType", ConfigManager.settings.connectionType.ordinal());
        tmp.put("ip", ConfigManager.settings.ip);
        tmp.put("gateway", ConfigManager.settings.gateway);
        tmp.put("netmask", ConfigManager.settings.netmask);
        tmp.put("hostname", ConfigManager.settings.hostname);
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalCameraSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        VisionProcess currentVisionProcess = VisionManager.getCurrentUIVisionProcess();
        CameraCapture currentCamera = VisionManager.getCurrentUIVisionProcess().getCamera();
        tmp.put("fov", currentCamera.getProperties().getFOV());
        tmp.put("streamDivisor", currentVisionProcess.cameraStreamer.getDivisor().ordinal());
        tmp.put("resolution", currentVisionProcess.getCamera().getProperties().getCurrentVideoModeIndex());
        return tmp;
    }

    public static void sendFullSettings() {
        //General settings
        Map<String, Object> fullSettings = new HashMap<>();

        VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
        CVPipeline currentPipeline = currentProcess.pipelineManager.getCurrentPipeline();

        try {
            fullSettings.put("settings", getOrdinalSettings());
            fullSettings.put("cameraSettings", getOrdinalCameraSettings());
            fullSettings.put("cameraList", VisionManager.getAllCameraNicknames());
            fullSettings.put("pipeline", getOrdinalPipeline(currentPipeline.settings.getClass()));
            fullSettings.put("pipelineList", VisionManager.getCurrentCameraPipelineNicknames());
            fullSettings.put("resolutionList", VisionManager.getCurrentCameraResolutionList());
            fullSettings.put("port", currentProcess.cameraStreamer.getStreamPort());
            fullSettings.put("currentPipelineIndex", VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipelineIndex());
            fullSettings.put("currentCameraIndex", VisionManager.getCurrentUIVisionProcessIndex());
        } catch (IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

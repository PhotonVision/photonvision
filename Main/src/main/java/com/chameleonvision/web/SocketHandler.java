package com.chameleonvision.web;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.pipeline.CVPipeline;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                CVPipeline currentPipeline = currentProcess.getCurrentPipeline();

                switch (entry.getKey()) {
                    case "driverMode": {
                        HashMap<String, Object> data = (HashMap<String, Object>) entry.getValue();
                        currentProcess.getDriverModeSettings().exposure = (Integer) data.get("exposure");
                        currentProcess.getDriverModeSettings().brightness = (Integer) data.get("brightness");
                        currentProcess.setDriverMode((Boolean) data.get("isDriver"));

                        VisionManager.saveCurrentCameraDriverMode();
                        break;
                    }
                    case "changeCameraName": {
                        currentCamera.getProperties().setNickname((String) entry.getValue());
                        sendFullSettings();
                        VisionManager.saveCurrentCameraSettings();
                        break;
                    }
                    case "changePipelineName": {
                        currentPipeline.settings.nickname = ((String) entry.getValue());
                        sendFullSettings();
                        VisionManager.saveCurrentCameraPipelines();
                        break;
                    }
                    case "duplicatePipeline": { // TODO doesn't work (HIGH)
                        HashMap pipelineVals = (HashMap) entry.getValue();
                        int pipelineIndex = (int) pipelineVals.get("pipeline");
                        int cameraIndex = (int) pipelineVals.get("camera");

                        CVPipeline origPipeline = currentProcess.getPipelineByIndex(pipelineIndex);

                        if (cameraIndex != -1) {
                            VisionProcess newProcess = VisionManager.getVisionProcessByIndex(cameraIndex);
                            if(newProcess != null) {
                                newProcess.addPipeline(origPipeline);
                            }
                        } else {
                            currentProcess.addPipeline(origPipeline);
                        }
                        VisionManager.saveCurrentCameraPipelines();
                        break;
                    }
                    case "command": {
                        switch ((String) entry.getValue()) {
                            case "addNewPipeline":
                                currentProcess.addBlankPipeline();
                                sendFullSettings();
                                VisionManager.saveCurrentCameraPipelines();
                                break;
                            // TODO: (HIGH) this never worked before, re-visit now that VisionProcess is written sanely
                            case "deleteCurrentPipeline":
//                                int currentIndex = currentProcess.getCurrentPipelineIndex();
//                                int nextIndex;
//                                if (currentIndex == currentProcess.getPipelines().size() - 1) {
//                                    nextIndex = currentIndex - 1;
//                                } else {
//                                    nextIndex = currentIndex;
//                                }
//                                cam.deletePipeline();
//                                cam.setCurrentPipelineIndex(nextIndex);
//                                sendFullSettings();
//                                VisionManager.saveCurrentCameraPipelines();
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
                        currentProcess.setPipeline((Integer) entry.getValue(), true);
                        sendFullSettings();
                        break;
                    }
                    default: {
                        setField(currentPipeline.settings, entry.getKey(), entry.getValue());
                        switch (entry.getKey()) {
                            case "exposure": {
                                currentCamera.setExposure((Integer) entry.getValue());
                            }
                            case "brightness": {
                                currentCamera.setBrightness((Integer) entry.getValue());
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
        for (Field field :cvClass.getFields()) { // iterate over every field in CVPipelineSettings
            try {
                if (!field.getType().isEnum()) { // if the field is not an enum, get it based on the current pipeline
                    tmp.put(field.getName(), field.get(VisionManager.getCurrentUIVisionProcess().getCurrentPipeline().settings));
                } else {
                    var ordinal = (Enum) field.get(VisionManager.getCurrentUIVisionProcess().getCurrentPipeline().settings);
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
        // TODO: (HIGH) get videomode index!
//            tmp.put("resolution", currentCamera.getVideoModeIndex());
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalDriver() {
        HashMap<String, Object> tmp = new HashMap<>();
        VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
        CVPipelineSettings driverModeSettings = currentProcess.getDriverModeSettings();
        tmp.put("isDriver", currentProcess.getDriverMode());
        tmp.put("driverBrightness", driverModeSettings.brightness);
        tmp.put("driverExposure", driverModeSettings.exposure);
        return tmp;
    }

    public static void sendFullSettings() {
        //General settings
        Map<String, Object> fullSettings = new HashMap<>();

        VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
        CameraCapture currentCamera = currentProcess.getCamera();
        CVPipeline currentPipeline = currentProcess.getCurrentPipeline();

        try {
//            fullSettings.putAll(settingsToMap(ConfigManager.settings));
//            fullSettings.putAll(pipelineToMap(currentPipeline.settings));
            fullSettings.put("settings", getOrdinalSettings());
            fullSettings.put("cameraSettings", getOrdinalCameraSettings());
            fullSettings.put("cameraList", VisionManager.getAllCameraNicknames());
            fullSettings.put("pipeline", getOrdinalPipeline(currentPipeline.settings.getClass()));
            fullSettings.put("driverMode", getOrdinalDriver());
            fullSettings.put("pipelineList", VisionManager.getCurrentCameraPipelineNicknames());
            fullSettings.put("resolutionList", VisionManager.getCurrentCameraResolutionList());
            fullSettings.put("port", currentProcess.cameraStreamer.getStreamPort());
            fullSettings.put("currentPipelineIndex", VisionManager.getCurrentUIVisionProcess().getCurrentPipelineIndex());
            fullSettings.put("currentCameraIndex", VisionManager.getCurrentUIVisionProcessIndex());
        } catch (IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

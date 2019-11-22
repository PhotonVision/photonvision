package com.chameleonvision.web;

import com.chameleonvision.classabstraction.VisionManager;
import com.chameleonvision.classabstraction.config.ConfigManager;
import com.chameleonvision.vision.*;
import com.chameleonvision.vision.camera.USBCamera;
import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.*;

import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;


public class ServerHandler {

    private static List<WsContext> users;
    private static ObjectMapper objectMapper;

    ServerHandler() {
        users = new ArrayList<>();
        objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    void onConnect(WsConnectContext context) {
        users.add(context);
        sendFullSettings();
    }

    public void onClose(WsCloseContext context) {
        users.remove(context);
    }

    void onBinaryMessage(WsBinaryMessageContext context) throws Exception {
        Map<String, Object> deserialized = objectMapper.readValue(ArrayUtils.toPrimitive(context.data()), new TypeReference<Map<String, Object>>() {
        });
        for (Map.Entry<String, Object> entry : deserialized.entrySet()) {
            try {
                switch (entry.getKey()) {
                    case "generalSettings": {
                        for (HashMap.Entry<String, Object> e : ((HashMap<String, Object>) entry.getValue()).entrySet()) {
                            setField(ConfigManager.settings, e.getKey(), e.getValue());
                        }
                        SettingsManager.saveSettings();
                        sendFullSettings();
                        break;
                    }
                    case "driverMode": {
                        for (HashMap.Entry<String, Object> e : ((HashMap<String, Object>) entry.getValue()).entrySet()) {
                            setField(VisionManager.getCurrentCamera(), e.getKey(), e.getValue());
                        }
                        VisionManager.getCurrentCamera().setDriverMode((Boolean) ((HashMap<String, Object>) entry.getValue()).get("isDriver"));
                        VisionManager.saveCameras();
                        break;
                    }
                    case "cameraSettings": {
                        HashMap camSettings = (HashMap) entry.getValue();
                        var curCam = VisionManager.getCurrentCamera();

                        Number newFOV = (Number) camSettings.get("fov");
                        Integer newStreamDivisor = (Integer) camSettings.get("streamDivisor");
                        Integer newResolution = (Integer) camSettings.get("resolution");

                        curCam.setFOV(newFOV);

                        var currentStreamDivisorOrdinal = curCam.getStreamDivisor().ordinal();
                        if (currentStreamDivisorOrdinal != newStreamDivisor) {
                            curCam.setStreamDivisor(newStreamDivisor, true);
                        }

                        var currentResolutionIndex = curCam.getVideoModeIndex();
                        if (currentResolutionIndex != newResolution) {
                            curCam.setCamVideoMode(newResolution, true);
                        }

                        VisionManager.saveCameras();
                        sendFullSettings();
                        break;
                    }
                    case "changeCameraName": {
                        VisionManager.getCurrentCamera().setNickname((String) entry.getValue());
                        sendFullSettings();
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "changePipelineName": {
                        VisionManager.getCurrentPipeline().nickname = (String) entry.getValue();
                        sendFullSettings();
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "duplicatePipeline": {
                        HashMap pipelineVals = (HashMap) entry.getValue();
                        int pipelineIndex = (int) pipelineVals.get("pipeline");
                        int cameraIndex = (int) pipelineVals.get("camera");

                        Pipeline origPipeline = VisionManager.getCurrentCamera().getPipelineByIndex(pipelineIndex);

                        if (cameraIndex != -1) {
                            VisionManager.getCameraByIndex(cameraIndex).addPipeline(origPipeline);
                        } else {
                            VisionManager.getCurrentCamera().addPipeline(origPipeline);
                        }
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "command": {
                        var cam = VisionManager.getCurrentCamera();
                        switch ((String) entry.getValue()) {
                            case "addNewPipeline":
                                cam.addPipeline();
                                sendFullSettings();
                                SettingsManager.saveSettings();
                                break;
                            case "deleteCurrentPipeline":
                                int currentIndex = cam.getCurrentPipelineIndex();
                                int nextIndex;
                                if (currentIndex == cam.getPipelines().size() - 1) {
                                    nextIndex = currentIndex - 1;
                                } else {
                                    nextIndex = currentIndex;
                                }
                                cam.deletePipeline();
                                cam.setCurrentPipelineIndex(nextIndex);
                                sendFullSettings();
                                SettingsManager.saveSettings();
                                break;
                            case "save":
                                SettingsManager.saveSettings();
                                System.out.println("saved Settings");
                                break;
                        }
                        // used to define all incoming commands
                        break;
                    }
                    case "currentCamera": {
                        VisionManager.setCurrentCamera((Integer) entry.getValue());
                        sendFullSettings();
                        break;
                    }
                    case "currentPipeline": {
                        var cam = VisionManager.getCurrentCamera();
                        cam.setCurrentPipelineIndex((Integer) entry.getValue());
                        sendFullSettings();
                        try {
                            cam.setBrightness(cam.getCurrentPipeline().brightness);
                            cam.setExposure(cam.getCurrentPipeline().exposure);
                        } catch (Exception e) {
                            continue;
                        }
                        break;
                    }
                    default: {
                        setField(VisionManager.getCurrentCamera().getCurrentPipeline(), entry.getKey(), entry.getValue());
                        switch (entry.getKey()) {
                            case "exposure": {
                                VisionManager.getCurrentCamera().setExposure((Integer) entry.getValue());
                            }
                            case "brightness": {
                                VisionManager.getCurrentCamera().setBrightness((Integer) entry.getValue());
                            }
                        }
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
            if (obj instanceof USBCamera) {
                var cam = (USBCamera)obj;
                if (fieldName.equals("driverBrightness")) {
                    cam.setDriverBrightness((Integer)value);
                } else if (fieldName.equals("driverExposure")) {
                    cam.setDriverExposure((Integer)value);
                }
            }
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
            for (var user : users) {
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

    private static HashMap<String, Object> getOrdinalPipeline() throws CameraException, IllegalAccessException {
        HashMap<String, Object> tmp = new HashMap<>();
        for (Field f : Pipeline.class.getFields()) {
            if (!f.getType().isEnum()) {
                tmp.put(f.getName(), f.get(VisionManager.getCurrentCamera().getCurrentPipeline()));
            } else {
                var i = (Enum) f.get(VisionManager.getCurrentCamera().getCurrentPipeline());
                tmp.put(f.getName(), i.ordinal());
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
        try {
            var currentCamera = VisionManager.getCurrentCamera();
            tmp.put("fov", currentCamera.getFOV());
            tmp.put("streamDivisor", currentCamera.getStreamDivisor().ordinal());
            tmp.put("resolution", currentCamera.getVideoModeIndex());
        } catch (CameraException e) {
            e.printStackTrace();
        }
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalDriver() {
        HashMap<String, Object> tmp = new HashMap<>();
        try {
            var currentCamera = VisionManager.getCurrentCamera();
            tmp.put("isDriver", currentCamera.getDriverMode());
            tmp.put("driverBrightness", currentCamera.getDriverBrightness());
            tmp.put("driverExposure", currentCamera.getDriverExposure());
        } catch (CameraException e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public static void sendFullSettings() {
        //General settings
        Map<String, Object> fullSettings = new HashMap<>();
        try {
            fullSettings.put("settings", getOrdinalSettings());
            fullSettings.put("cameraSettings", getOrdinalCameraSettings());
            fullSettings.put("cameraList", VisionManager.getAllCameraByNickname());
            fullSettings.put("pipeline", getOrdinalPipeline());
            var currentCamera = VisionManager.getCurrentCamera();
            fullSettings.put("driverMode",getOrdinalDriver());
            fullSettings.put("pipelineList", currentCamera.getPipelinesNickname());
            fullSettings.put("resolutionList", currentCamera.getResolutionList());
            fullSettings.put("port", currentCamera.getStreamPort());
            fullSettings.put("currentPipelineIndex", VisionManager.getCurrentCamera().getCurrentPipelineIndex());
            fullSettings.put("currentCameraIndex", VisionManager.getCurrentCameraIndex());
        } catch (CameraException | IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

package com.chameleonvision.web;

import com.chameleonvision.settings.GeneralSettings;
import com.chameleonvision.vision.*;
import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoException;
import io.javalin.websocket.*;

import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
                            setField(SettingsManager.GeneralSettings, e.getKey(), e.getValue());
                        }
                        SettingsManager.saveSettings();
                        sendFullSettings();
                        break;
                    }
                    case "driverMode": {
                        for (HashMap.Entry<String, Object> e : ((HashMap<String, Object>) entry.getValue()).entrySet()) {
                            setField(CameraManager.getCurrentCamera(), e.getKey(), e.getValue());
                        }
                        CameraManager.getCurrentCamera().setDriverMode((Boolean) ((HashMap<String, Object>) entry.getValue()).get("isDriver"));
                        CameraManager.saveCameras();
                        break;
                    }
                    case "cameraSettings": {
                        HashMap camSettings = (HashMap) entry.getValue();
                        var curCam = CameraManager.getCurrentCamera();

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

                        CameraManager.saveCameras();
                        sendFullSettings();
                        break;
                    }
                    case "changeCameraName": {
                        CameraManager.getCurrentCamera().setNickname((String) entry.getValue());
                        sendFullSettings();
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "changePipelineName": {
                        CameraManager.getCurrentPipeline().nickname = (String) entry.getValue();
                        sendFullSettings();
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "duplicatePipeline": {
                        HashMap pipelineVals = (HashMap) entry.getValue();
                        int pipelineIndex = (int) pipelineVals.get("pipeline");
                        int cameraIndex = (int) pipelineVals.get("camera");

                        Pipeline origPipeline = CameraManager.getCurrentCamera().getPipelineByIndex(pipelineIndex);

                        if (cameraIndex != -1) {
                            CameraManager.getCameraByIndex(cameraIndex).addPipeline(origPipeline);
                        } else {
                            CameraManager.getCurrentCamera().addPipeline(origPipeline);
                        }
                        SettingsManager.saveSettings();
                        break;
                    }
                    case "command": {
                        var cam = CameraManager.getCurrentCamera();
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
                        CameraManager.setCurrentCamera((Integer) entry.getValue());
                        sendFullSettings();
                        break;
                    }
                    case "currentPipeline": {
                        var cam = CameraManager.getCurrentCamera();
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
                        setField(CameraManager.getCurrentCamera().getCurrentPipeline(), entry.getKey(), entry.getValue());
                        switch (entry.getKey()) {
                            case "exposure": {
                                CameraManager.getCurrentCamera().setExposure((Integer) entry.getValue());
                            }
                            case "brightness": {
                                CameraManager.getCurrentCamera().setBrightness((Integer) entry.getValue());
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
            if (obj instanceof Camera) {
                var cam = (Camera)obj;
                switch (fieldName) {
                    case "driverBrightness":
                        cam.setDriverBrightness((Integer) value);
                        break;
                    case "driverExposure":
                        cam.setDriverExposure((Integer) value);
                        break;
                    default:
                        Field field = obj.getClass().getField(fieldName);
                        if (field.getType().isEnum()) {
                            field.set(obj, field.getType().getEnumConstants()[(Integer) value]);
                        } else {
                            field.set(obj, value);
                        }
                        break;
                }
            } else {
                Field field = obj.getClass().getField(fieldName);
                if (field.getType().isEnum()) {
                    field.set(obj, field.getType().getEnumConstants()[(Integer) value]);
                }
                else {
                    field.set(obj, value);
                }
            }

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
                tmp.put(f.getName(), f.get(CameraManager.getCurrentCamera().getCurrentPipeline()));
            } else {
                var i = (Enum) f.get(CameraManager.getCurrentCamera().getCurrentPipeline());
                tmp.put(f.getName(), i.ordinal());
            }
        }
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", SettingsManager.GeneralSettings.teamNumber);
        tmp.put("connectionType", SettingsManager.GeneralSettings.connectionType.ordinal());
        tmp.put("ip", SettingsManager.GeneralSettings.ip);
        tmp.put("gateway", SettingsManager.GeneralSettings.gateway);
        tmp.put("netmask", SettingsManager.GeneralSettings.netmask);
        tmp.put("hostname", SettingsManager.GeneralSettings.hostname);
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalCameraSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        try {
            var currentCamera = CameraManager.getCurrentCamera();
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
            var currentCamera = CameraManager.getCurrentCamera();
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
            fullSettings.put("cameraList", CameraManager.getAllCameraByNickname());
            fullSettings.put("pipeline", getOrdinalPipeline());
            var currentCamera = CameraManager.getCurrentCamera();
            fullSettings.put("driverMode",getOrdinalDriver());
            fullSettings.put("pipelineList", currentCamera.getPipelinesNickname());
            fullSettings.put("resolutionList", currentCamera.getResolutionList());
            fullSettings.put("port", currentCamera.getStreamPort());
            fullSettings.put("currentPipelineIndex", CameraManager.getCurrentCamera().getCurrentPipelineIndex());
            fullSettings.put("currentCameraIndex", CameraManager.getCurrentCameraIndex());
        } catch (CameraException | IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

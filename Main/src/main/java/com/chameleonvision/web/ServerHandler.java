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
import org.springframework.beans.BeanUtils;

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
        Map<String, Object> deserialized = objectMapper.readValue(ArrayUtils.toPrimitive(context.data()), new TypeReference<Map<String,Object>>(){});
        for (Map.Entry<String,Object> entry: deserialized.entrySet()) {
            try {
                switch (entry.getKey()) {
                    case "generalSettings": {
                        for (HashMap.Entry<String,Object> e : ((HashMap<String,Object>)entry.getValue()).entrySet()){
                            setField(SettingsManager.GeneralSettings,e.getKey(),e.getValue());
                        }
                        break;
                    }
                    case "cameraSettings": {
                        HashMap camSettings = (HashMap)entry.getValue();
                        CameraManager.getCurrentCamera().setFOV((Number)camSettings.get("fov"));
                        CameraManager.getCurrentCamera().setStreamDivisor((Integer) camSettings.get("streamDivisor"));
                        CameraManager.getCurrentCamera().setCamVideoMode((Integer) camSettings.get("resolution"),true);
                        break;
                    }
                    case "changeCameraName":{
                        // needs to be implemented
                    }
                    case "changePipelineName":{
                        // needs to be implemented
                    }
                    case "duplicatePipeline":{
                        // needs to be implemented
                    }
                    case "command": {
                        switch ((String) entry.getValue()){
                            case "addNewPipeline":
                                CameraManager.getCurrentCamera().addPipeline();
                                break;
                            case "deleteCurrentPipeline":
                                var cam = CameraManager.getCurrentCamera();
                                CameraManager.getCurrentCamera().deleteCurrentPipeline();
                                CameraManager.getCurrentCamera().setCurrentPipelineIndex(cam.getCurrentPipelineIndex() -1);
                                sendFullSettings();
                                break;
                        }
                        // used to define all incoming commands
                        break;
                    }
                    case "currentCamera": {
                        CameraManager.setCurrentCamera((String) entry.getValue());
                        HashMap<String,Object> tmp = new HashMap<>();
                        tmp.put("pipeline",CameraManager.getCurrentCamera().getCurrentPipeline());
                        tmp.put("port", CameraManager.getCurrentCamera().getStreamPort());
                        tmp.put("resolutionList",CameraManager.getResolutionList());
                        broadcastMessage(tmp);
                        break;
                    }
                    case "currentPipeline": {
                        CameraManager.getCurrentCamera().setCurrentPipelineIndex((Integer) entry.getValue());
                        HashMap<String,Object> tmp = new HashMap<>();
                        tmp.put("pipeline",getOrdinalPipeline());
                        broadcastMessage(tmp);
                        break;
                    }
                    default: {
                        setField(CameraManager.getCurrentCamera().getCurrentPipeline(),entry.getKey(),entry.getValue());
                        switch (entry.getKey()){
                            case "exposure":{
                                try{
                                    CameraManager.getCurrentCamera().setExposure((Integer) entry.getValue());
                                } catch (Exception e){
                                    System.err.println("Camera Does not support exposure change");
                                }
                            }
                            case "brightness":{
                                CameraManager.getCurrentCamera().setBrightness((Integer) entry.getValue());
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            broadcastMessage(deserialized,context);
        }
    }
    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getField(fieldName);
            if (BeanUtils.isSimpleValueType(field.getType())){
                if (field.getType().isEnum()){
                    field.set(obj,field.getType().getEnumConstants()[(Integer) value]);
                }else{
                    field.set(obj,value);
                }
            } else if(field.getType() == List.class){
//                if(((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0] == Double.class){
                    field.set(obj,value);
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
                try{
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

    private static HashMap<String,Object> getOrdinalPipeline() throws CameraException, IllegalAccessException {
        HashMap<String,Object> tmp = new HashMap<>();
        for (Field f : Pipeline.class.getFields()){
            if (!f.getType().isEnum()){
                tmp.put(f.getName(),f.get(CameraManager.getCurrentCamera().getCurrentPipeline()));
            } else{
                var i = (Enum) f.get(CameraManager.getCurrentCamera().getCurrentPipeline());
                tmp.put(f.getName(),i.ordinal());
            }
        }
        return tmp;
    }
    private static HashMap<String,Object> getOrdinalSettings(){
        HashMap<String,Object> tmp = new HashMap<>();
        tmp.put("teamNumber",SettingsManager.GeneralSettings.teamNumber);
        tmp.put("connectionType",SettingsManager.GeneralSettings.connectionType.ordinal());
        tmp.put("ip",SettingsManager.GeneralSettings.ip);
        tmp.put("gateway",SettingsManager.GeneralSettings.gateway);
        tmp.put("netmask",SettingsManager.GeneralSettings.netmask);
        tmp.put("hostname",SettingsManager.GeneralSettings.hostname);
        return tmp;
    }
    private static HashMap<String ,Object> getOrdinalCameraSettings(){
        HashMap<String,Object> tmp = new HashMap<>();
        try {
            var currentCamera = CameraManager.getCurrentCamera();
            tmp.put("fov",currentCamera.getFOV());
            tmp.put("streamDivisor",currentCamera.getStreamDivisor().ordinal());
            tmp.put("resolution",currentCamera.getVideoModeIndex());
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
            fullSettings.put("cameraSettings",getOrdinalCameraSettings());
            fullSettings.put("cameraList", CameraManager.getAllCamerasByName().keySet());
            var currentCamera = CameraManager.getCurrentCamera();
            fullSettings.put("pipeline", getOrdinalPipeline());
            fullSettings.put("pipelineList", currentCamera.getPipelines().keySet());
            fullSettings.put("resolutionList", CameraManager.getResolutionList());
            fullSettings.put("port", currentCamera.getStreamPort());
        } catch (CameraException | IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

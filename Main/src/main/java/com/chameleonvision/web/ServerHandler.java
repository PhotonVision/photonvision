package com.chameleonvision.web;

import com.chameleonvision.settings.GeneralSettings;
import com.chameleonvision.vision.Orientation;
import com.chameleonvision.vision.SortMode;
import com.chameleonvision.vision.TargetGroup;
import com.chameleonvision.vision.TargetIntersection;
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
                        System.out.println("asdds");
                        //change general settings using a general settings object
                        break;
                    }
                    case "cameraSettings": {
                        System.out.println("sadfsdf");
                        //change camera settings using a camera settings object
                        break;
                    }
                    case "command": {
                        System.err.println("not implemented");
                        // used to define all incoming commands
                        break;
                    }
                    case "currentCamera": {
                        CameraManager.setCurrentCamera((String) entry.getValue());
                        HashMap<String,Object> tmp = new HashMap<>();
                        tmp.put("pipeline",CameraManager.getCurrentCamera().getCurrentPipeline());
                        broadcastMessage(tmp);
                        break;
                    }
                    case "currentPipeline": {
                        CameraManager.getCurrentCamera().setCurrentPipelineIndex((Integer) entry.getValue());
                        HashMap<String,Object> tmp = new HashMap<>();
                        tmp.put("pipeline",CameraManager.getCurrentCamera().getCurrentPipeline());
                        //TODO Add cam settings to the map
                        broadcastMessage(tmp);
                        break;
                    }
                    default: {
                        setField(CameraManager.getCurrentCamera().getCurrentPipeline(),entry.getKey(),entry.getValue());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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

    public static void broadcastMessage(Object obj) {//TODO fix sending for msgpack
        broadcastMessage(obj, null);//Broadcasts the message to every user
    }


    public static void sendFullSettings() {
        //General settings
        Map<String, Object> fullSettings = new HashMap<String, Object>();
        fullSettings.put("settings", SettingsManager.GeneralSettings);
        fullSettings.put("cameraList", CameraManager.getAllCamerasByName().keySet());
        try {
            var currentCamera = CameraManager.getCurrentCamera();
            fullSettings.put("pipeline",currentCamera.getCurrentPipeline());
            fullSettings.put("pipelineList", currentCamera.getPipelines().keySet());
            fullSettings.put("resolutionList", CameraManager.getResolutionList());
            fullSettings.put("port", currentCamera.getStreamPort());
        } catch (CameraException e) {
            System.err.println("No camera found!");
            //TODO: add message to ui to inform that there are no cameras
        }
        broadcastMessage(fullSettings);
    }
}

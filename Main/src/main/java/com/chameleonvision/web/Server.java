package com.chameleonvision.web;

import com.chameleonvision.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import edu.wpi.cscore.VideoException;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.lang.reflect.Field;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;


public class Server {
    private static List<WsContext> users = new ArrayList<>();

    public static void main(int port) {
        Javalin app = Javalin.create();
        app.config.addStaticFiles("web");
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                users.add(ctx);
                System.out.println("Socket Connected");
                sendFullSettings();
            });
            ws.onClose(ctx -> {
                users.remove(ctx);
                System.out.println("Socket Disconnected");
                SettingsManager.getInstance().SaveSettings();
            });
            ws.onMessage(ctx -> {
                broadcastMessage(ctx.message(), ctx);



                JSONObject jsonObject = new JSONObject(ctx.message());
                String key = null;
                var jsonKeySetArray = jsonObject.keySet().toArray();
                try {
                    key = jsonKeySetArray[0].toString();
                } catch (Exception ex) {
                    System.err.println("WebSocket JSON data was empty!");
                }
                if (key == null) return;
                Object value = jsonObject.get(key);
//                System.out.printf("Got websocket json data: [%s, %s]\n", key, value);
                if (!allFieldsToMap(CameraManager.getCurrentPipeline()).containsKey(key)) {
                    //If field not in pipeline
                    switch (key) {
                        case "change_general_settings_values":
                            JSONObject newSettings = (JSONObject) value;
                            setFields(SettingsManager.GeneralSettings, newSettings);
                            break;
                        case "curr_camera":
                            String newCamera = (String) value;
                            System.out.printf("Changing camera to %s\n", newCamera);
                            CameraManager.setCurrentCamera(newCamera);
                            //broadcastMessage((Map<String, Object>) new HashMap<String, Object>(){}.put("port",SettingsManager.CameraPorts.get(SettingsManager.GeneralSettings.curr_camera)));
                            broadcastMessage(CameraManager.getCurrentCamera()); //TODO CHECK JSON FOR CAMERA CHANGE
                            break;
                        case "curr_pipeline":
                            String newPipeline = (String) value;
                            var pipelineNumber = Integer.parseInt(newPipeline.replace("pipeline", ""));
                            System.out.printf("Changing pipeline to %s\n", newPipeline);
                            CameraManager.setCurrentPipeline(pipelineNumber);
                            broadcastMessage(allFieldsToMap(CameraManager.getCurrentPipeline()));
                            break;
                        case "resolution":
                            int newVideoMode = (int) value;
                            System.out.printf("Changing video mode to %d\n", newVideoMode);
                            CameraManager.getCurrentCamera().setCamVideoMode(newVideoMode);
                            break;
                        case "FOV":
                            double newFov = (double) value;
                            System.out.printf("Changing FOV to %f\n", newFov);
                            CameraManager.getCurrentCamera().setFOV(newFov);
                            break;
                        default:
                            System.out.printf("Unexpected value from websocket: [%s, %s]\n", key, value);
                            break;
                    }
                } else {
                    setField(CameraManager.getCurrentPipeline(), key, value);
                    //Special cases for exposure and brightness
                    //TODO maybe add listener for value changes instead of this special case
                    switch (key) {
                        case "exposure":
                            int newExposure = (int) value;
                            System.out.printf("Changing exposure to %d\n", newExposure);
                            try {
                                CameraManager.getCurrentCamera().setExposure(newExposure);
                            }
                            catch ( VideoException e)
                            {
                                System.out.println("Exposure changes is not supported on your webcam/webcam's driver");
                            }
                            //TODO check if this crash occurs on linux
                            break;
                        case "brightness":
                            int newBrightness = (int) value;
                            System.out.printf("Changing brightness to %d\n", newBrightness);
                            CameraManager.getCurrentCamera().setBrightness(newBrightness);
                            break;
                    }
                }
            });
        });
        app.start(port);
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fieldName)) {
                    if (BeanUtils.isSimpleValueType(value.getClass())) {
                        f.set(obj, value);
                    } else if (value.getClass() == JSONArray.class) {
                        f.set(obj, ((JSONArray) value).toList());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            System.out.println("IllegalAccessException ");
            e.printStackTrace();
        }
    }

    private static void setFields(Object obj, JSONObject data) {
        Map<String,Object> map = data.toMap();
        map.forEach((s, o) -> setField(obj,s,o));
    }


    private static void broadcastMessage(Object obj, WsContext userToSkip) {//TODO chekc if session id is a good way to differentiate users
        for (var user : users) {
            if (userToSkip != null && user.getSessionId().equals(userToSkip.getSessionId())) {
                continue;
            }
            if (obj.getClass() == String.class)
                user.send((String) obj);
            else if (obj.getClass() == HashMap.class)
                user.send(new JSONObject((HashMap<String, Object>) obj).toString());
            else
                user.send(new JSONObject(obj).toString());
        }
    }

    public static void broadcastMessage(Object obj) {
        broadcastMessage(obj, null);//Broadcasts the message to ever user
    }

    private static Map<String, Object> allFieldsToMap(Object obj) {
        Map map = new HashMap<String, Object>();
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field field : fields) {
                map.put(field.getName(), field.get(obj));
            }
        } catch (IllegalAccessException e) {
            System.err.println("Illegal Access error:" + e.getStackTrace());
        }
        return map;
    }

    private static void sendFullSettings() {
        //General settings
        Map<String, Object> fullSettings = new HashMap<>(allFieldsToMap(SettingsManager.GeneralSettings));
        fullSettings.put("cameraList", CameraManager.getAllCamerasByName().keySet());
        try {
            var currentCamera = CameraManager.getCurrentCamera();
            fullSettings.putAll(allFieldsToMap(currentCamera.getCurrentPipeline()));
            fullSettings.put("pipelineList", currentCamera.getPipelines().keySet());
            fullSettings.put("resolutionList", CameraManager.getResolutionList());
            fullSettings.put("resolution", currentCamera.getVideoModeIndex());
            fullSettings.put("FOV", currentCamera.getFOV());
//            fullSettings.put("port", SettingsManager.CameraPorts.get(SettingsManager.GeneralSettings.curr_camera));
        } catch (CameraException e) {
            System.err.println("No camera found!");
            //TODO: add message to ui to inform that there are no cameras
        }
        broadcastMessage(fullSettings);
    }


}

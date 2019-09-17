package com.chameleonvision.web;

import com.chameleonvision.NoCameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.GeneralSettings;
import com.chameleonvision.vision.Pipeline;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.lang.reflect.Field;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;


public class Server {
    private static List<WsContext> users = new ArrayList<WsContext>();

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
//                System.out.println(SettingsManager.getInstance().GetCurrentPipeline().);
                broadcastMessage(ctx, ctx.message());
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
                if (!setField(SettingsManager.getInstance().GetCurrentPipeline(), key, value)) {
                    //If field not in pipeline
                    switch (key) {
                        case "change_general_settings_values":
                            JSONObject newSettings = (JSONObject) value;
                            setFields(SettingsManager.GeneralSettings, newSettings);
                            break;
                        case "curr_camera":
                            String newCamera = (String) value;
                            System.out.printf("Changing camera to %s\n", newCamera);
                            SettingsManager.getInstance().SetCurrentCamera(newCamera);
                            //broadcastMessage((Map<String, Object>) new HashMap<String, Object>(){}.put("port",SettingsManager.CameraPorts.get(SettingsManager.GeneralSettings.curr_camera)));
                            //broadcastMessage(ctx, SettingsManager.getInstance().GetCurrentCamera()); //TODO CHECK JSON FOR CAMERA CHANGE
                            break;
                        case "curr_pipeline":
                            String newPipeline = (String) value;
                            System.out.printf("Changing pipeline to %s\n", newPipeline);
                            SettingsManager.getInstance().SetCurrentPipeline(newPipeline);
                            SettingsManager.CamerasCurrentPipeline.put(SettingsManager.GeneralSettings.curr_camera, newPipeline);
                            break;
                        case "resolution":
                            int newResolution = (int) value;
                            System.out.printf("Changing resolution mode to %d\n", newResolution);
                            SettingsManager.getInstance().GetCurrentCamera().resolution = newResolution;
                            SettingsManager.getInstance().SetCameraSettings(SettingsManager.GeneralSettings.curr_camera, "resolution", newResolution);
                            SettingsManager.getInstance().SaveSettings();
                            break;
                        case "fov":
                            double newFov = (double) value;
                            System.out.printf("Changing FOV to %d\n", newFov);
                            SettingsManager.getInstance().GetCurrentCamera().FOV = newFov;
                            SettingsManager.getInstance().SaveSettings();
                            break;
                        default:
                            System.out.printf("Unexpected value from websocket: [%s, %s]\n", key, value);
                            break;
                    }
                } else { //
                    switch (key) {
                        case "exposure":
                            int newExposure = (int) value;
                            System.out.printf("Changing exposure to %d\n", newExposure);
                            SettingsManager.getInstance().GetCurrentPipeline().exposure = newExposure;
                            SettingsManager.getInstance().GetCurrentUsbCamera().setExposureManual(newExposure);
                            SettingsManager.getInstance().SaveSettings();
                            break;
                        case "brightness":
                            int newBrightness = (int) value;
                            System.out.printf("Changing brightness to %d\n", newBrightness);
                            SettingsManager.getInstance().GetCurrentPipeline().brightness = newBrightness;
                            SettingsManager.getInstance().GetCurrentUsbCamera().setBrightness(newBrightness);
                            SettingsManager.getInstance().SaveSettings();
                            break;
                    }
                }
            });
        });
        app.start(port);
    }


    public static boolean setField(Object obj, String fieldName, Object value) {
        boolean successful = false;
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fieldName)) {
                    successful = true;
                    if (BeanUtils.isSimpleValueType(value.getClass())) {
                        f.set(obj, value);
                    } else if (value.getClass() == JSONArray.class) {
                        f.set(obj, ((JSONArray) value).toList());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return successful;
    }

    public static boolean setFields(Object obj, JSONObject data) {
        boolean successful = false;
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (data.has(f.getName())) {
                    Object value = data.get(f.getName());
                    if (BeanUtils.isSimpleValueType(value.getClass())) {
                        successful = true;
                        f.set(obj, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return successful;
    }

    private static void broadcastMessage(WsContext sendingUser, Object obj) {//TODO chekc if session id is a good way to differentiate users
        for (var user : users) {
            if (sendingUser!=null&& user.getSessionId().equals(sendingUser.getSessionId())) {
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

    private static void addAllFieldsToMap(Map<String, Object> map, Object obj) {
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field field : fields)
                map.put(field.getName(), field.get(obj));
        } catch (IllegalAccessException e) {
            System.err.println("Illegal Access error:" + e.getStackTrace().toString());
        }
    }

    private static void sendFullSettings() {
        Map<String, Object> fullSettings = new HashMap<>();
        //General settings
        addAllFieldsToMap(fullSettings, SettingsManager.GeneralSettings);
        fullSettings.put("cameraList", SettingsManager.Cameras.keySet());
        try {
            addAllFieldsToMap(fullSettings, SettingsManager.getInstance().GetCurrentPipeline());
            fullSettings.put("pipelineList", SettingsManager.getInstance().GetCurrentCamera().pipelines.keySet());
            fullSettings.put("resolutionList", SettingsManager.getInstance().GetResolutionList());
            fullSettings.put("resolution", SettingsManager.getInstance().GetCurrentCamera().resolution);
            fullSettings.put("FOV", SettingsManager.getInstance().GetCurrentCamera().FOV);
//            fullSettings.put("port", SettingsManager.CameraPorts.get(SettingsManager.GeneralSettings.curr_camera));
        } catch (NoCameraException e) {
            System.err.println("No camera found!");
            //TODO: add message to ui to inform that there are no cameras
        }
        broadcastMessage(null, fullSettings);
    }


}

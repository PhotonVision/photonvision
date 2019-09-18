package com.chameleonvision.web;

import com.chameleonvision.NoCameraException;
import com.chameleonvision.settings.SettingsManager;
import edu.wpi.cscore.VideoException;
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
                if (!allFieldsToMap(SettingsManager.getInstance().GetCurrentPipeline()).containsKey(key)) {
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
                            broadcastMessage(SettingsManager.getInstance().GetCurrentCamera()); //TODO CHECK JSON FOR CAMERA CHANGE
                            break;
                        case "curr_pipeline":
                            String newPipeline = (String) value;
                            System.out.printf("Changing pipeline to %s\n", newPipeline);
                            SettingsManager.getInstance().SetCurrentPipeline(newPipeline);
                            SettingsManager.CamerasCurrentPipeline.put(SettingsManager.GeneralSettings.curr_camera, newPipeline);
                            broadcastMessage(allFieldsToMap(SettingsManager.getInstance().GetCurrentPipeline()));
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
                            System.out.printf("Changing FOV to %f\n", newFov);
                            SettingsManager.getInstance().GetCurrentCamera().FOV = newFov;
                            SettingsManager.getInstance().SaveSettings();
                            break;
                        default:
                            System.out.printf("Unexpected value from websocket: [%s, %s]\n", key, value);
                            break;
                    }
                } else {
                    setField(SettingsManager.getInstance().GetCurrentPipeline(), key, value);
                    //Special cases for exposure and brightness
                    //TODO maybe add listener for value changes instead of this special case
                    switch (key) {
                        case "exposure":
                            int newExposure = (int) value;
                            System.out.printf("Changing exposure to %d\n", newExposure);
                            SettingsManager.getInstance().GetCurrentPipeline().exposure = newExposure;
                            try {
                                SettingsManager.getInstance().GetCurrentUsbCamera().setExposureManual(newExposure);
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
                            SettingsManager.getInstance().GetCurrentPipeline().brightness = newBrightness;
                            SettingsManager.getInstance().GetCurrentUsbCamera().setBrightness(newBrightness);
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

    private static void broadcastMessage(Object obj) {
        broadcastMessage(obj, null);//Broadcasts the message to ever user
    }

    private static Map<String, Object> allFieldsToMap(Object obj) {
        Map map = new HashMap<>();
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field field : fields)
                map.put(field.getName(), field.get(obj));
        } catch (IllegalAccessException e) {
            System.err.println("Illegal Access error:" + e.getStackTrace().toString());
        }
        return map;
    }

    private static void sendFullSettings() {
        Map<String, Object> fullSettings = new HashMap<>();
        //General settings
        fullSettings.putAll(allFieldsToMap(SettingsManager.GeneralSettings));
        fullSettings.put("cameraList", SettingsManager.Cameras.keySet());
        try {
            fullSettings.putAll(allFieldsToMap(SettingsManager.getInstance().GetCurrentPipeline()));
            fullSettings.put("pipelineList", SettingsManager.getInstance().GetCurrentCamera().pipelines.keySet());
            fullSettings.put("resolutionList", SettingsManager.getInstance().GetResolutionList());
            fullSettings.put("resolution", SettingsManager.getInstance().GetCurrentCamera().resolution);
            fullSettings.put("FOV", SettingsManager.getInstance().GetCurrentCamera().FOV);
//            fullSettings.put("port", SettingsManager.CameraPorts.get(SettingsManager.GeneralSettings.curr_camera));
        } catch (NoCameraException e) {
            System.err.println("No camera found!");
            //TODO: add message to ui to inform that there are no cameras
        }
        broadcastMessage(fullSettings);
    }


}

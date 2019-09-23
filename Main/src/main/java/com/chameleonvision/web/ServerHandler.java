package com.chameleonvision.web;

import com.chameleonvision.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.camera.CameraManager;
import com.google.gson.JsonArray;
import edu.wpi.cscore.VideoException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler {

    private static List<WsContext> users;

    ServerHandler() {
        users = new ArrayList<>();
    }

    void onConnect(WsConnectContext context) {
        users.add(context);
        sendFullSettings();
    }

    public void onClose(WsCloseContext context) {
        users.remove(context);
    }

    void onMessage(WsMessageContext data) throws CameraException {
        broadcastMessage(data.message(), data);

        JSONObject jsonObject = new JSONObject(data.message());
        String key = null;
        var jsonKeySetArray = jsonObject.keySet().toArray();
        try {
            key = jsonKeySetArray[0].toString();
        } catch (Exception ex) {
            System.err.println("WebSocket JSON data was empty!");
        }
        if (key == null) return;
        Object value = jsonObject.get(key);
        System.out.printf("Got websocket json data: [%s, %s]\n", key, value);
        if (hasField(CameraManager.getCurrentPipeline(), key)) {
            //Special cases for exposure and brightness and aspect ratio
            switch (key) {
                case "exposure":
                    int newExposure = (int) value;
                    System.out.printf("Changing exposure to %d\n", newExposure);
                    try {
                        CameraManager.getCurrentCamera().setExposure(newExposure);
                    } catch (VideoException e) {
                        System.out.println("Exposure changes is not supported on your webcam/webcam's driver");
                    }
                    break;
                case "brightness":
                    int newBrightness = (int) value;
                    System.out.printf("Changing brightness to %d\n", newBrightness);
                    CameraManager.getCurrentCamera().setBrightness(newBrightness);
                    break;
                case "ratio":
                    //If there is any better to convert Integer to Double you're welcome to change it
                    List<Double> doubleRatio = CameraManager.getCurrentPipeline().ratio;
                    List<Object> newRatio = ((JSONArray) value).toList();
                    for (int i = 0; i < newRatio.size(); i++) {
                        doubleRatio.set(i, Double.parseDouble(newRatio.get(i).toString()));
                    }
                    break;
                default:
                    //Any other field in CameraManager that doesn't need anything special
                    setField(CameraManager.getCurrentPipeline(), key, value);
                    break;
            }
        } else {
            switch (key) {
                case "change_general_settings_values":
                    JSONObject newSettings = (JSONObject) value;
//                    setFields(SettingsManager.GeneralSettings, newSettings);
                    Map<String, Object> map = newSettings.toMap();
                    map.forEach((s, o) -> setField(SettingsManager.GeneralSettings, s, o));
                    break;
                case "curr_camera":
                    String newCamera = (String) value;
                    System.out.printf("Changing camera to %s\n", newCamera);
                    CameraManager.setCurrentCamera(newCamera);
                    HashMap<String, Integer> portMap = new HashMap<String, Integer>();
                    portMap.put("port", CameraManager.getCurrentCamera().getStreamPort());
                    broadcastMessage(portMap);
                    broadcastMessage(CameraManager.getCurrentCamera()); //TODO CHECK JSON FOR CAMERA CHANGE
                    break;
                case "curr_pipeline":
                    String newPipeline = (String) value;
                    var pipelineNumber = Integer.parseInt(newPipeline.replace("pipeline", ""));
                    System.out.printf("Changing pipeline to %s\n", newPipeline);
                    CameraManager.setCurrentPipeline(pipelineNumber);
//                    broadcastMessage(allFieldsToMap(CameraManager.getCurrentPipeline()));
                    broadcastMessage(allFieldsToMap(CameraManager.getCurrentPipeline()));
                    break;
                case "resolution":
                    int newVideoMode = (int) value;
                    System.out.printf("Changing video mode to %d\n", newVideoMode);
                    CameraManager.getCurrentCamera().setCamVideoMode(newVideoMode, true);
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
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
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


    private static void broadcastMessage(Object obj, WsContext userToSkip) {//TODO check if session id is a good way to differentiate users
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


    private boolean hasField(Object obj, String fieldName) {
        Field[] fields = obj.getClass().getFields();
        for (Field field : fields) {
            if (fieldName.equals(field.getName()))
                return true;
        }
        return false;
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

    public static void sendFullSettings() {
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
            fullSettings.put("port", currentCamera.getStreamPort());
        } catch (CameraException e) {
            System.err.println("No camera found!");
            //TODO: add message to ui to inform that there are no cameras
        }
        broadcastMessage(fullSettings);
    }
}

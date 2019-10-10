package com.chameleonvision.web;

import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import edu.wpi.cscore.VideoException;
import io.javalin.websocket.*;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.BooleanValue;
import org.msgpack.type.IntegerValue;
import org.msgpack.type.MapValue;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ServerHandler {

    private static List<WsContext> users;
    private MessagePack msgpack;

    ServerHandler() {
        users = new ArrayList<>();
        msgpack = new MessagePack();
    }

    void onConnect(WsConnectContext context) {
        users.add(context);
        sendFullSettings();
    }

    public void onClose(WsCloseContext context) {
        users.remove(context);
    }

    void onMessage(WsBinaryMessageContext data) throws IOException {
        byte[] b = ArrayUtils.toPrimitive(data.data());
        System.out.println(msgpack.read(b).isMapValue());
        Map entries = msgpack.read(b,Templates.tMap(Templates.TString,Templates.TValue));
        System.out.println(Arrays.toString(entries.entrySet().toArray()));
        entries.forEach((k, value) -> {
            /*
             To get int from value
             ((IntegerValue)value).getInt();

             To get boolean from value
             ((BooleanValue)value).getBoolean();

             To get Array from value
             ((ArrayValue) value).toArray();

            */
            String key = k.toString();
            System.out.printf("Got websocket msgpack data: [%s, %s]\n", key, value);


            try{
            if (hasField(CameraManager.getCurrentPipeline(), key)) {
                //Special cases for exposure and brightness and aspect ratio
                switch (key) {
                    case "exposure":
                        int newExposure =  ((IntegerValue)value).getInt();
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
                        System.out.println("CameraManager.getCurrentPipeline().hue = " + CameraManager.getCurrentPipeline().hue.get(0));
                        break;
                }
            } else {
                switch (key) {
                    case "change_general_settings_values":
                        Map<String, Object> map = (Map<String, Object>) value;
                        map.forEach((s, o) -> setField(SettingsManager.GeneralSettings, s, o));
                        SettingsManager.saveSettings();
                        break;
                    case "curr_camera":
                        String newCamera = (String) value;
                        System.out.printf("Changing camera to %s\n", newCamera);
                        CameraManager.setCurrentCamera(newCamera);
                        HashMap<String, Integer> portMap = new HashMap<>();
                        portMap.put("port", CameraManager.getCurrentCamera().getStreamPort());
                        sendFullSettings();
                        break;
                    case "curr_pipeline":
                        int newPipeline = (int) value;
                        System.out.printf("Changing pipeline to %s\n", newPipeline);
                        CameraManager.setCurrentPipeline(newPipeline);
                        CameraManager.getCurrentCameraProcess().ntPipelineEntry.setNumber(newPipeline);
                        broadcastMessage(allFieldsToMap(CameraManager.getCurrentPipeline()));
                        break;
                    case "resolution":
                        int newVideoMode = (int) value;
                        System.out.printf("Changing video mode to %d\n", newVideoMode);
                        CameraManager.getCurrentCamera().setCamVideoMode(newVideoMode, true);
                        break;
                    case "FOV":
                        double newFov = Double.parseDouble(value.toString());//TODO check this
                        System.out.printf("Changing FOV to %f\n", newFov);
                        CameraManager.getCurrentCamera().setFOV(newFov);
                        break;
                    default:
                        System.out.printf("Unexpected value from websocket: [%s, %s]\n", key, value);
                        break;
                }}
            }
            catch (CameraException e)
            {
                System.err.println("Camera error");
                e.printStackTrace();
            }
        });
        broadcastMessage(entries, data);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fieldName)) {
                    if (BeanUtils.isSimpleValueType(value.getClass())) {
                        f.set(obj, value);
                    } else if (value.getClass() == ArrayValue.class) {
                        f.set(obj, ((ArrayValue) value).toArray());
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
            user.send(obj);
//            if (obj.getClass() == String.class)
//                user.send((String) obj);
//            else if (obj.getClass() == HashMap.class)
//                user.send(new JSONObject((HashMap<String, Object>) obj).toString());
//            else
//                user.send(new JSONObject(obj).toString());
        }
    }

    public static void broadcastMessage(Object obj) {//TODO fix sending for msgpack
        broadcastMessage(obj, null);//Broadcasts the message to every user
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

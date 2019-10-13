package com.chameleonvision.web;

import com.chameleonvision.vision.Orientation;
import com.chameleonvision.vision.SortMode;
import com.chameleonvision.vision.TargetGroup;
import com.chameleonvision.vision.TargetIntersection;
import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import edu.wpi.cscore.VideoException;
import io.javalin.websocket.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableArrayValue;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.Value;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.*;


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

    void onBinaryMessage(WsBinaryMessageContext data) throws Exception {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(ArrayUtils.toPrimitive(data.data()));
        int length = unpacker.unpackMapHeader();
        for (int mapIndex = 0; mapIndex < length; mapIndex++) {
            String key = unpacker.unpackString();  // key
//            Object value = get(unpacker.unpackValue());
            ImmutableValue value = unpacker.unpackValue();
            try {
                switch (key) {
                    case "generalSettings": {
                        //change general settings using a general settings object
                        break;
                    }
                    case "cameraSettings": {
                        //change camera settings using a camera settings object
                        break;
                    }
                    case "command": {
                        // used to define all incoming commands
                        break;
                    }
                    case "currentCamera": {
                        //camera name by string
                        break;
                    }
                    case "currentPipeline": {
                        // camera pipeline by index
                        break;
                    }
                    default: {//Change pipeline values
                        //Two special cases for exposure and brightness changes
                        if (key.equals("exposure"))
                            try {
                                CameraManager.getCurrentCamera().setExposure(value.asIntegerValue().toInt());
                            } catch (VideoException e) {
                                System.out.println("Exposure changes is not supported on your webcam/webcam's driver");
                            }
                        else if (key.equals("brightness")) try {
                            CameraManager.getCurrentCamera().setBrightness(value.asIntegerValue().toInt());
                        } catch (VideoException e) {
                            e.printStackTrace();
                        }
                        else
                            setValue(CameraManager.getCurrentPipeline(), key, value);//All of the other assignments fields
                        broadcastMessage(data.data());
                        System.out.println(ToStringBuilder.reflectionToString(CameraManager.getCurrentPipeline()));//Print all pipeline data for debugging
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                unexpectedData(key, value);
            }
        }

    }

    private void setValue(Object obj, String fieldName, ImmutableValue value) {
        try {
            boolean found = false;
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fieldName)) {
                    found = true;
                    if (f.getType().isEnum())//Field is enum like Orientation
                        f.set(obj, f.getType().getEnumConstants()[value.asIntegerValue().toInt()]);
                    else if (value.isBooleanValue()) {//Field is boolean like erode
                        f.set(obj, value.asBooleanValue().getBoolean());
                    } else if (value.isIntegerValue()) {//Field is int like M and B
                        f.set(obj, value.asIntegerValue().toInt());
                    } else if (f.get(obj) instanceof List<?>) {
                        List<Value> valLst = ((ImmutableArrayValue) value).list();
                        if (((List) f.get(obj)).get(0).getClass().equals(Float.class)) {//Field is List of Floats like area in pipeline
                            List<Float> lst = new ArrayList<>();
                            for (Value v : valLst) {
                                lst.add(v.isFloatValue() ? v.asFloatValue().toFloat() : (float) v.asIntegerValue().toInt());//Adds float if value is float, casts value to float from int otherwise
                            }
                            f.set(obj, lst);
                        } else if (((List) f.get(obj)).get(0).getClass().equals(Integer.class)) {//Fields is List of Integers like hue in pipeline
                            List<Integer> lst = new ArrayList<>();
                            for (Value v : valLst) {
                                lst.add(v.asIntegerValue().toInt());
                            }
                            f.set(obj, lst);
                        }
                    }
                }
            }
            if (!found)
                unexpectedData(fieldName, value);
        } catch (Exception e) {
            System.out.println("Exception setting field");
            e.printStackTrace();
        }
    }

    public void unexpectedData(String key, ImmutableValue v) {
        System.err.println("Unexpected key or value, key=" + key + " Value=" + v.toString());
        //TODO send a error message to the user
        //TODO in the very far future send a bug report?
    }

    private static void broadcastMessage(Object obj, WsContext userToSkip) {
        if (users != null)
            for (var user : users) {
                if (userToSkip != null && user.getSessionId().equals(userToSkip.getSessionId())) {
                    continue;
                }
                user.send(obj);
            }
    }

    public static void broadcastMessage(Object obj) {//TODO fix sending for msgpack
        broadcastMessage(obj, null);//Broadcasts the message to every user
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
//        broadcastMessage(fullSettings);
    }
}

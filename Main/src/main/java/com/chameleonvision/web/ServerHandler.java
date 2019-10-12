package com.chameleonvision.web;

import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import io.javalin.websocket.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableArrayValue;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
            Object value = get(unpacker.unpackValue());
            runMethod(ApplyFields.class, "set" + key, value);
        }
        System.out.println(ReflectionToStringBuilder.toString(CameraManager.getCurrentPipeline()));
    }

    private void runMethod(Class clazz, String funcName, Object param) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (funcName.equalsIgnoreCase(method.getName())) {
                    method.invoke(null, param);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Error while trying to call a function");
            e.printStackTrace();
        }
    }


//    private void setField(Object obj, String fieldName, Object value) {
//        try {
//            Field[] fields = obj.getClass().getFields();
//            for (Field f : fields) {
//                if (f.getName().equals(fieldName)) {
//                    if (BeanUtils.isSimpleValueType(f.getType())) {
//                        f.set(obj, value);
//                    } else if (value instanceof ImmutableArrayValue) {
//                        List<Value> valLst = ((ImmutableArrayValue) value).list();
//                        List<Object> lst = new ArrayList<>();
//                        for (Value val : valLst) {
//                            lst.add(get((ImmutableValue) val));
//                        }
//                        f.set(obj, lst);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("Exception setting field ");
//            e.printStackTrace();
//        }
//    }
//
//    public List<Object> getList(ImmutableArrayValue value) {
//        List<Value> valLst = value.list();
//        List<Object> lst = new ArrayList<>();
//        for (Value val : valLst) {
//            try {
//                lst.add(get((ImmutableValue) val));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return lst;
//    }

    private Object get(ImmutableValue v) throws Exception {
        //TODO find a smarter way to write this
        if (v.isIntegerValue())
            return v.asIntegerValue().toInt();
        if (v.isFloatValue())
            return v.asFloatValue().toFloat();
        if (v.isArrayValue()) {
            return v.asArrayValue();
        }
        if (v.isBinaryValue())
            return v.asBinaryValue().asByteArray();
        if (v.isBooleanValue())
            return v.asBooleanValue().getBoolean();
        if (v.isMapValue())
            return v.asMapValue().map();
        if (v.isStringValue())
            return v.asStringValue().asString();
        throw new Exception("Value not recognized");
    }

    public List<Float> getFloatList(ImmutableArrayValue arrayValue) {
        List<Float> output = new ArrayList<>();
        List<Value> values = arrayValue.list();
        for (Value v : values) {
            if (v.isFloatValue())
                output.add(v.asFloatValue().toFloat());
            else
                output.add((float) v.asIntegerValue().toInt());
        }
        return output;
    }

    public List<Integer > getIntList(ImmutableArrayValue arrayValue) {
        List<Integer> output = new ArrayList<>();
        List<Value> values = arrayValue.list();
        for (Value v : values) {
            output.add((int) v.asIntegerValue().toInt());
        }
        return output;
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


//    private boolean hasField(Object obj, String fieldName) {
//        Field[] fields = obj.getClass().getFields();
//        for (Field field : fields) {
//            if (fieldName.equals(field.getName()))
//                return true;
//        }
//        return false;
//    }

    public static Map<String, Object> allFieldsToMap(Object obj) {
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

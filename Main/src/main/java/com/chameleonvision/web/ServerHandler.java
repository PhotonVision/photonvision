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
            Object value = get(unpacker.unpackValue());
            try {
                if (hasField(CameraManager.getCurrentPipeline(), key)) {
                    //Special cases for exposure and brightness and aspect ratio
                    switch (key) {
                        case "exposure":
                            int newExposure = (int) value;
                            System.out.printf("Changing exposure to %d\n", newExposure);
                            try {
                                ;
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
                            CameraManager.getCurrentPipeline().ratio=getFloatList((ImmutableArrayValue) value);
                            break;
                        case "area":
                            CameraManager.getCurrentPipeline().area=getFloatList((ImmutableArrayValue) value);
                            break;
                        //Enums
                        case "targetIntersection":
                            setField(CameraManager.getCurrentPipeline(), key, TargetIntersection.values()[(int) value]);
                            break;
                        case "targetGroup":
                            setField(CameraManager.getCurrentPipeline(), key, TargetGroup.values()[(int) value]);
                            break;
                        case "sortMode":
                            setField(CameraManager.getCurrentPipeline(), key, SortMode.values()[(int) value]);
                            break;
                        case "orientation":
                            setField(CameraManager.getCurrentPipeline(), key, Orientation.values()[(int) value]);
                            break;
                        default:
                            //Any other field in CameraManager that doesn't need anything special
                            setField(CameraManager.getCurrentPipeline(), key, value);
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
                            float newFov = ((Integer) value) * 1F;//TODO check this
                            System.out.printf("Changing FOV to %f\n", newFov);
                            CameraManager.getCurrentCamera().setFOV(newFov);
                            break;
                        default:
                            System.out.printf("Unexpected value from websocket: [%s, %s]\n", key, value);
                            break;
                    }
                }
            } catch (CameraException e) {
                System.err.println("Camera error");
                e.printStackTrace();
            }
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field[] fields = obj.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fieldName)) {
                    if (BeanUtils.isSimpleValueType(f.getType())) {
                        f.set(obj, value);
                    } else if (value instanceof ImmutableArrayValue) {
                        List<Value> valLst = ((ImmutableArrayValue) value).list();
                        List<Object> lst = new ArrayList<>();
                        for (Value val : valLst) {
                            lst.add(get((ImmutableValue) val));
                        }
                        f.set(obj, lst);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception setting field ");
            e.printStackTrace();
        }
    }

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

    private List<Float> getFloatList(ImmutableArrayValue arrayValue) {
        List<Float> output = new ArrayList<>();
        List<Value> values = arrayValue.list();
        for (Value v:values) {
            if (v.isFloatValue())
                output.add(v.asFloatValue().toFloat());
            else
                output.add((float) v.asIntegerValue().toInt());
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

package com.chameleonvision._2.web;

import com.chameleonvision._2.config.CameraCalibrationConfig;
import com.chameleonvision._2.config.ConfigManager;
import com.chameleonvision._2.vision.VisionManager;
import com.chameleonvision._2.vision.VisionProcess;
import com.chameleonvision._2.vision.camera.CameraCapture;
import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import com.chameleonvision._2.vision.camera.USBCameraCapture;
import com.chameleonvision._2.vision.enums.ImageRotationMode;
import com.chameleonvision._2.vision.enums.StreamDivisor;
import com.chameleonvision._2.vision.pipeline.CVPipeline;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoMode;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class SocketHandler {

    private static List<WsContext> users;
    private static ObjectMapper objectMapper;

    private static final Object broadcastLock = new Object();

    SocketHandler() {
        users = new ArrayList<>();
        objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    void onConnect(WsConnectContext context) {
        users.add(context);
        sendFullSettings();
    }

    void onClose(WsCloseContext context) {
        users.remove(context);
    }

    @SuppressWarnings("unchecked")
    void onBinaryMessage(WsBinaryMessageContext context) throws Exception {
        Map<String, Object> deserialized =
                objectMapper.readValue(
                        (byte[]) ArrayUtils.toPrimitive(context.data()), new TypeReference<>() {});
        for (Map.Entry<String, Object> entry : deserialized.entrySet()) {
            try {
                VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
                CameraCapture currentCamera = currentProcess.getCamera();
                CVPipeline currentPipeline = currentProcess.pipelineManager.getCurrentPipeline();
                //                System.out.println("entry.getKey()+entry.getValue()= " + entry.getKey() +
                // entry.getValue());
                switch (entry.getKey()) {
                    case "driverMode":
                        {
                            HashMap<String, Object> data = (HashMap<String, Object>) entry.getValue();
                            currentProcess.getDriverModeSettings().exposure =
                                    (Integer) data.get("driverExposure");
                            currentProcess.getDriverModeSettings().brightness =
                                    (Integer) data.get("driverBrightness");
                            currentProcess.setDriverMode((Boolean) data.get("isDriver"));

                            VisionManager.saveCurrentCameraDriverMode();
                            break;
                        }
                    case "changeCameraName":
                        {
                            currentProcess.setCameraNickname((String) entry.getValue());
                            sendFullSettings();
                            VisionManager.saveCurrentCameraSettings();
                            break;
                        }
                    case "changePipelineName":
                        {
                            currentProcess.pipelineManager.renameCurrentPipeline((String) entry.getValue());
                            sendFullSettings();
                            VisionManager.saveCurrentCameraPipelines();
                            break;
                        }
                    case "addNewPipeline":
                        {
                            //                        HashMap<String, Object> data = (HashMap<String, Object>)
                            // entry.getValue();
                            String pipeName = (String) entry.getValue();
                            // TODO: add to UI selection for new 2d/3d
                            currentProcess.pipelineManager.addNewPipeline(pipeName);
                            sendFullSettings();
                            VisionManager.saveCurrentCameraPipelines();
                            break;
                        }
                    case "command":
                        {
                            switch ((String) entry.getValue()) {
                                case "deleteCurrentPipeline":
                                    currentProcess.pipelineManager.deleteCurrentPipeline();
                                    sendFullSettings();
                                    VisionManager.saveCurrentCameraPipelines();
                                    break;
                                case "save":
                                    ConfigManager.saveGeneralSettings();
                                    VisionManager.saveAllCameras();
                                    System.out.println("Saved Settings");
                                    break;
                            }
                            // used to define all incoming commands
                            break;
                        }
                    case "currentCamera":
                        {
                            VisionManager.setCurrentProcessByIndex((Integer) entry.getValue());
                            sendFullSettings();
                            break;
                        }
                    case "is3D":
                        {
                            VisionManager.getCurrentUIVisionProcess().setIs3d((Boolean) entry.getValue());
                            break;
                        }
                    case "currentPipeline":
                        {
                            currentProcess.pipelineManager.setCurrentPipeline((Integer) entry.getValue());
                            sendFullSettings();
                            break;
                        }
                    case "isPNPCalibration":
                        {
                            currentProcess.pipelineManager.setCalibrationMode((Boolean) entry.getValue());
                            break;
                        }
                    case "takeCalibrationSnapshot":
                        {
                            currentProcess.pipelineManager.calib3dPipe.takeSnapshot();
                        }
                    default:
                        {
                            switch (entry.getKey()) { // Pre field value set
                                case "rotationMode":
                                    { // Create new CaptureStaticProperties with new width and height, reset crosshair
                                        // calib
                                        ImageRotationMode oldRot = currentPipeline.settings.rotationMode;
                                        ImageRotationMode newRot =
                                                ImageRotationMode.class.getEnumConstants()[(Integer) entry.getValue()];
                                        CaptureStaticProperties prop =
                                                currentCamera.getProperties().getStaticProperties();
                                        int width, height;
                                        if (oldRot.isRotated() != newRot.isRotated()) {
                                            width = prop.mode.height;
                                            height = prop.mode.width;
                                            // Creates new video mode with new width and height to create new
                                            // CaptureStaticProperties and applies it
                                            currentCamera
                                                    .getProperties()
                                                    .setStaticProperties(
                                                            new CaptureStaticProperties(
                                                                    new VideoMode(
                                                                            prop.mode.pixelFormat, width, height, prop.mode.fps),
                                                                    prop.fov));
                                        }
                                        prop = currentCamera.getProperties().getStaticProperties();
                                        currentProcess.cameraStreamer.recalculateDivision();
                                        if (currentPipeline instanceof StandardCVPipeline)
                                            ((StandardCVPipeline) currentPipeline)
                                                    .settings.point.set(
                                                            prop.mode.width / 2.0,
                                                            prop.mode.height / 2.0); // Reset Crosshair in single point calib
                                        break;
                                    }
                            }

                            if (currentProcess.pipelineManager.getDriverMode()) {
                                setField(
                                        currentProcess.pipelineManager.driverModePipeline.settings,
                                        entry.getKey(),
                                        entry.getValue());
                            } else {
                                setField(currentPipeline.settings, entry.getKey(), entry.getValue());
                            }

                            // Post field value set
                            switch (entry.getKey()) {
                                case "exposure":
                                    {
                                        currentCamera.setExposure((Integer) entry.getValue());
                                        break;
                                    }
                                case "brightness":
                                    {
                                        currentCamera.setBrightness((Integer) entry.getValue());
                                        break;
                                    }
                                case "gain":
                                    {
                                        currentCamera.setGain((Integer) entry.getValue());
                                        break;
                                    }
                                case "videoModeIndex":
                                    {
                                        if (currentPipeline instanceof StandardCVPipeline)
                                            ((StandardCVPipeline) currentPipeline).settings.point =
                                                    new DoubleCouple(); // This will reset the calibration
                                        currentCamera.setVideoMode((Integer) entry.getValue());
                                        currentProcess.cameraStreamer.recalculateDivision();
                                        break;
                                    }
                                case "streamDivisor":
                                    {
                                        currentProcess.cameraStreamer.setDivisor(
                                                StreamDivisor.values()[(Integer) entry.getValue()], true);
                                        break;
                                    }
                            }

                            VisionManager.saveCurrentCameraPipelines();
                            break;
                        }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            broadcastMessage(deserialized, context);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getField(fieldName);
            if (field.getType().isEnum())
                field.set(obj, field.getType().getEnumConstants()[(Integer) value]);
            else field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    private static void broadcastMessage(Object obj, WsContext userToSkip) {
        synchronized (broadcastLock) {
            if (users != null) {
                var userList = users;
                for (WsContext user : userList) {
                    if (userToSkip != null && user.getSessionId().equals(userToSkip.getSessionId())) {
                        continue;
                    }
                    try {
                        ByteBuffer b = ByteBuffer.wrap(objectMapper.writeValueAsBytes(obj));
                        user.send(b);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void broadcastMessage(Object obj) {
        broadcastMessage(obj, null); // Broadcasts the message to every user
    }

    private static HashMap<String, Object> getOrdinalPipeline(Class cvClass)
            throws IllegalAccessException {
        HashMap<String, Object> tmp = new HashMap<>();
        for (Field field : cvClass.getFields()) { // iterate over every field in CVPipelineSettings
            try {
                if (!field
                        .getType()
                        .isEnum()) { // if the field is not an enum, get it based on the current pipeline
                    tmp.put(
                            field.getName(),
                            field.get(
                                    VisionManager.getCurrentUIVisionProcess()
                                            .pipelineManager
                                            .getCurrentPipeline()
                                            .settings));
                } else {
                    var ordinal =
                            (Enum)
                                    field.get(
                                            VisionManager.getCurrentUIVisionProcess()
                                                    .pipelineManager
                                                    .getCurrentPipeline()
                                                    .settings);
                    tmp.put(field.getName(), ordinal.ordinal());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("teamNumber", ConfigManager.settings.teamNumber);
        tmp.put("connectionType", ConfigManager.settings.connectionType.ordinal());
        tmp.put("ip", ConfigManager.settings.ip);
        tmp.put("gateway", ConfigManager.settings.gateway);
        tmp.put("netmask", ConfigManager.settings.netmask);
        tmp.put("hostname", ConfigManager.settings.hostname);
        return tmp;
    }

    private static HashMap<String, Object> getOrdinalCameraSettings() {
        HashMap<String, Object> tmp = new HashMap<>();
        VisionProcess currentVisionProcess = VisionManager.getCurrentUIVisionProcess();
        USBCameraCapture currentCamera = VisionManager.getCurrentUIVisionProcess().getCamera();

        tmp.put("fov", currentCamera.getProperties().getFOV());
        tmp.put("streamDivisor", currentVisionProcess.cameraStreamer.getDivisor().ordinal());
        tmp.put(
                "resolution", currentVisionProcess.getCamera().getProperties().getCurrentVideoModeIndex());
        tmp.put("tilt", currentVisionProcess.getCamera().getProperties().getTilt().getDegrees());

        List<CameraCalibrationConfig.UICameraCalibrationConfig> calibrations =
                currentCamera.getAllCalibrationData().stream()
                        .map(CameraCalibrationConfig.UICameraCalibrationConfig::new)
                        .collect(Collectors.toList());
        tmp.put("calibration", calibrations);

        return tmp;
    }

    public static void sendFullSettings() {
        // General settings
        Map<String, Object> fullSettings = new HashMap<>();

        VisionProcess currentProcess = VisionManager.getCurrentUIVisionProcess();
        CVPipeline currentPipeline = currentProcess.pipelineManager.getCurrentPipeline();

        try {
            fullSettings.put("settings", getOrdinalSettings());
            fullSettings.put("cameraSettings", getOrdinalCameraSettings());
            fullSettings.put("cameraList", VisionManager.getAllCameraNicknames());
            fullSettings.put("pipeline", getOrdinalPipeline(currentPipeline.settings.getClass()));
            fullSettings.put("pipelineList", VisionManager.getCurrentCameraPipelineNicknames());
            fullSettings.put("resolutionList", VisionManager.getCurrentCameraResolutionList());
            fullSettings.put("port", currentProcess.cameraStreamer.getStreamPort());
            fullSettings.put(
                    "currentPipelineIndex",
                    VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipelineIndex());
            fullSettings.put("currentCameraIndex", VisionManager.getCurrentUIVisionProcessIndex());
        } catch (IllegalAccessException e) {
            System.err.println("No camera found!");
        }
        broadcastMessage(fullSettings);
    }
}

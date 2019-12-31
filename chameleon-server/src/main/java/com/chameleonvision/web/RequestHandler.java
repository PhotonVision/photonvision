package com.chameleonvision.web;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.network.NetworkIPMode;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.camera.USBCameraCapture;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.vision.pipeline.PipelineManager;
import com.chameleonvision.vision.pipeline.impl.Calibrate3dPipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHandler {

    public static void onGeneralSettings(Context ctx) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map map = objectMapper.readValue(ctx.body(), Map.class);

            // TODO: change to function, to restart NetworkTables
            ConfigManager.settings.teamNumber = (int) map.get("teamNumber");

            ConfigManager.settings.connectionType = NetworkIPMode.values()[(int) map.get("connectionType")];
            ConfigManager.settings.ip = (String) map.get("ip");
            ConfigManager.settings.netmask = (String) map.get("netmask");
            ConfigManager.settings.gateway = (String) map.get("gateway");
            ConfigManager.settings.hostname = (String) map.get("hostname");
            ConfigManager.saveGeneralSettings();
            SocketHandler.sendFullSettings();
            ctx.status(200);
        } catch (JsonProcessingException e) {
            ctx.status(500);
        }
    }

    public static void onDuplicatePipeline(Context ctx) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map newPipelineData = objectMapper.readValue(ctx.body(), Map.class);

            int newCam = -1;
            try {
                newCam = (Integer) newPipelineData.get("camera");
            } catch (Exception e) {
                // ignored
            }

            var pipeline = (CVPipelineSettings) newPipelineData.get("pipeline");

            if (newCam == -1) {
                if (VisionManager.getCurrentCameraPipelineNicknames().contains(pipeline.nickname)) {
                    ctx.status(400); // BAD REQUEST
                } else {
                    VisionManager.getCurrentUIVisionProcess().pipelineManager.addPipeline(pipeline);
                    ctx.status(200);
                }
            } else {
                var cam = VisionManager.getVisionProcessByIndex(newCam);
                if (cam != null && cam.pipelineManager.pipelines.stream().anyMatch(c -> c.settings.nickname.equals(pipeline.nickname))) {
                    ctx.status(400); // BAD REQUEST
                } else {
                    cam.pipelineManager.addPipeline(pipeline);
                    ctx.status(200);
                }
            }

        } catch (JsonProcessingException e) {
            ctx.status(500);
        }
    }

    public static void onCameraSettings(Context ctx) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map camSettings = objectMapper.readValue(ctx.body(), Map.class);

            VisionProcess currentVisionProcess = VisionManager.getCurrentUIVisionProcess();
            USBCameraCapture currentCamera = currentVisionProcess.getCamera();

            double newFOV, tilt;
            try {
                newFOV = (Double) camSettings.get("fov");
            } catch (Exception ignored) {
                newFOV = (Integer) camSettings.get("fov");
            }
            try {
                tilt = (Double) camSettings.get("tilt");
            } catch (Exception ignored) {
                tilt = (Integer) camSettings.get("tilt");
            }
            currentCamera.getProperties().setFOV(newFOV);
            currentCamera.getProperties().setTilt(Rotation2d.fromDegrees(tilt));
            VisionManager.saveCurrentCameraSettings();
            SocketHandler.sendFullSettings();
            ctx.status(200);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            ctx.status(500);
        }
    }

    public static void onCalibrationStart(Context ctx) throws JsonProcessingException {
        PipelineManager pipeManager = VisionManager.getCurrentUIVisionProcess().pipelineManager;
        ObjectMapper objectMapper = new ObjectMapper();
        var data = objectMapper.readValue(ctx.body(), Map.class);
        int resolutionIndex = (Integer) data.get("resolution");
        double squareSize;
        try {
            squareSize = (Double) data.get("squareSize");
        } catch (Exception e) {
            squareSize = (Integer) data.get("squareSize");
        }
        // convert from mm to meters
        pipeManager.calib3dPipe.setSquareSize(squareSize / 1000d);
        VisionManager.getCurrentUIVisionProcess().pipelineManager.calib3dPipe.settings.videoModeIndex = resolutionIndex;
        VisionManager.getCurrentUIVisionProcess().pipelineManager.setCalibrationMode(true);
        VisionManager.getCurrentUIVisionProcess().getCamera().setVideoMode(resolutionIndex);
    }

    public static void onSnapshot(Context ctx) {
        Calibrate3dPipeline calPipe = VisionManager.getCurrentUIVisionProcess().pipelineManager.calib3dPipe;

        calPipe.takeSnapshot();

        HashMap<String, Object> toSend = new HashMap<>();
        toSend.put("snapshotCount", calPipe.getSnapshotCount());
        toSend.put("hasEnough", calPipe.hasEnoughSnapshots());

        ctx.json(toSend);
        ctx.status(200);
    }

    public static void onCalibrationEnding(Context ctx) throws JsonProcessingException {
        PipelineManager pipeManager = VisionManager.getCurrentUIVisionProcess().pipelineManager;
        System.out.println("Finishing Cal");
        if (pipeManager.calib3dPipe.hasEnoughSnapshots()) {
            if (pipeManager.calib3dPipe.tryCalibration()) {
                ctx.status(200);
            } else {
                System.err.println("CALFAIL");
                ctx.status(500);
            }
        }
        pipeManager.setCalibrationMode(false);
        ctx.status(200);
    }

    public static void onPnpModel(Context ctx) throws JsonProcessingException {
        System.out.println(ctx.body());
        ObjectMapper objectMapper = new ObjectMapper();
        List points = objectMapper.readValue(ctx.body(), List.class);
        System.out.println(points);
    }
}

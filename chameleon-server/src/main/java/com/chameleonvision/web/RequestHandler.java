package com.chameleonvision.web;

import com.chameleonvision.Exceptions.DuplicatedKeyException;
import com.chameleonvision.Main;
import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.network.NetworkIPMode;
import com.chameleonvision.networktables.NetworkTablesManager;
import com.chameleonvision.util.Helpers;
import com.chameleonvision.util.Platform;
import com.chameleonvision.util.ProgramDirectoryUtilities;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.camera.USBCameraCapture;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.vision.pipeline.PipelineManager;
import com.chameleonvision.vision.pipeline.impl.Calibrate3dPipeline;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipelineSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.math3.ml.neuralnet.Network;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHandler {

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    public static void onGeneralSettings(Context ctx) {
        ObjectMapper objectMapper = kObjectMapper;
        try {
            Map map = objectMapper.readValue(ctx.body(), Map.class);

            // TODO: change to function, to restart NetworkTables
            int newTeamNumber = (int) map.get("teamNumber");
            if (newTeamNumber != ConfigManager.settings.teamNumber && !NetworkTablesManager.isServer) {
                NetworkTablesManager.setTeamClientMode();
            }
            ConfigManager.settings.teamNumber = newTeamNumber;

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
        ObjectMapper objectMapper = kObjectMapper;
        try {
            Map data = objectMapper.readValue(ctx.body(), Map.class);

            int cameraIndex = (Integer) data.getOrDefault("camera", -1);

            var pipelineIndex = (Integer) data.get("pipeline");
            StandardCVPipelineSettings origPipeline = (StandardCVPipelineSettings) VisionManager.getCurrentUIVisionProcess().pipelineManager.getPipeline(pipelineIndex).settings;
            String tmp = objectMapper.writeValueAsString(origPipeline);
            StandardCVPipelineSettings newPipeline = objectMapper.readValue(tmp, StandardCVPipelineSettings.class);

            if (cameraIndex == -1) { // same camera

                VisionManager.getCurrentUIVisionProcess().pipelineManager.duplicatePipeline(newPipeline);

            } else { // another camera
                var cam = VisionManager.getVisionProcessByIndex(cameraIndex);
                if (cam != null) {
                    if (cam.getCamera().getProperties().videoModes.size() < newPipeline.videoModeIndex) {
                        newPipeline.videoModeIndex = cam.getCamera().getProperties().videoModes.size() - 1;
                    }
                    if (newPipeline.is3D) {
                        var calibration = cam.getCamera().getCalibration(cam.getCamera().getProperties().getVideoMode(newPipeline.videoModeIndex));
                        if (calibration == null) {
                            newPipeline.is3D = false;
                        }
                    }
                    VisionManager.getCurrentUIVisionProcess().pipelineManager.duplicatePipeline(newPipeline, cam);
                    ctx.status(200);
                } else {
                    ctx.status(500);
                }
            }
        } catch (JsonProcessingException | DuplicatedKeyException ex) {
            ctx.status(500);
        }
    }


    public static void onCameraSettings(Context ctx) {
        ObjectMapper objectMapper = kObjectMapper;
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
        ObjectMapper objectMapper = kObjectMapper;
        var data = objectMapper.readValue(ctx.body(), Map.class);
        int resolutionIndex = (Integer) data.get("resolution");
        double squareSize;
        try {
            squareSize = (Double) data.get("squareSize");
        } catch (Exception e) {
            squareSize = (Integer) data.get("squareSize");
        }
        // convert from mm to meters
        pipeManager.calib3dPipe.setSquareSize(squareSize);
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

        var data = kObjectMapper.readValue(ctx.body(), Map.class);
        double squareSize;
        try {
            squareSize = (Double) data.get("squareSize");
        } catch (Exception e) {
            squareSize = (Integer) data.get("squareSize");
        }
        pipeManager.calib3dPipe.setSquareSize(squareSize);

        System.out.println("Finishing Cal");
        if (pipeManager.calib3dPipe.hasEnoughSnapshots()) {
            if (pipeManager.calib3dPipe.tryCalibration()) {
                HashMap<String, Double> tmp = new HashMap<String, Double>();
                tmp.put("accuracy", pipeManager.calib3dPipe.getCalibrationAccuracy());
                ctx.json(tmp);
                ctx.status(200);
            } else {
                System.err.println("CALFAIL");
                ctx.status(500);
            }
        } else {
            pipeManager.setCalibrationMode(false);
            ctx.status(201);
        }
    }

    public static void onPnpModel(Context ctx) throws JsonProcessingException {
        //noinspection unchecked
        List<List<Number>> points = kObjectMapper.readValue(ctx.body(), List.class);
        try {
            // each entry should be an xy pair
            var pointsList = new ArrayList<Point3>();
            for (List<Number> point : points) {
                double x, y;
                x = point.get(0).doubleValue();
                y = point.get(1).doubleValue();
                var pointToAdd = new Point3(x, y, 0.0);
                pointsList.add(pointToAdd);
            }
            System.out.println(pointsList.toString());
            if (VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipeline().settings instanceof StandardCVPipelineSettings) {
                var settings = (StandardCVPipelineSettings) VisionManager.getCurrentUIVisionProcess().pipelineManager.getCurrentPipeline().settings;
                settings.targetCornerMat.fromList(pointsList);
            }
        } catch (Exception e) {
            ctx.status(500);
        }
    }

    public static void onInstallOrUpdate(Context ctx) {
        Platform p = Platform.getCurrentPlatform();
        try {
            if (p == Platform.LINUX_RASPBIAN || p == Platform.LINUX_64) {
                UploadedFile file = ctx.uploadedFile("file");
                Path filePath;
                if (file != null) {
                    filePath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), file.getFilename());
                    File target = new File(filePath.toString());
                    OutputStream stream = new FileOutputStream(target);
                    file.getContent().transferTo(stream);
                    stream.close();
                } else {
                    filePath = Paths.get(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath()); // quirk to get the current file directory
                }
                Helpers.setService(filePath);
                ctx.status(200);
            } else {
                ctx.result("Only Linux Platforms Support this feature");
                ctx.status(500);
            }
        } catch (Exception e) {
            ctx.result(e.toString());
            ctx.status(500);
        }
    }
}

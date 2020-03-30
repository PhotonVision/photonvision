package com.chameleonvision._2.web;

import com.chameleonvision._2.Main;
import com.chameleonvision._2.config.ConfigManager;
import com.chameleonvision._2.network.NetworkManager;
import com.chameleonvision._2.util.Helpers;
import com.chameleonvision._2.util.ProgramDirectoryUtilities;
import com.chameleonvision._2.vision.VisionManager;
import com.chameleonvision._2.vision.VisionProcess;
import com.chameleonvision._2.vision.camera.USBCameraCapture;
import com.chameleonvision._2.vision.pipeline.PipelineManager;
import com.chameleonvision._2.vision.pipeline.impl.Calibrate3dPipeline;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipelineSettings;
import com.chameleonvision.common.datatransfer.networktables.NetworkTablesManager;
import com.chameleonvision.common.networking.NetworkMode;
import com.chameleonvision.common.util.Platform;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.opencv.core.Point3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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

            ConfigManager.settings.connectionType = NetworkMode.values()[(int) map.get("connectionType")];
            ConfigManager.settings.ip = (String) map.get("ip");
            ConfigManager.settings.netmask = (String) map.get("netmask");
            ConfigManager.settings.gateway = (String) map.get("gateway");
            ConfigManager.settings.hostname = (String) map.get("hostname");
            ConfigManager.saveGeneralSettings();
            // setting up network config after saving
            boolean isStatic = ConfigManager.settings.connectionType.equals(NetworkMode.STATIC);

            boolean state = NetworkManager.setHostname(ConfigManager.settings.hostname) && NetworkManager.setNetwork(isStatic, ConfigManager.settings.ip, ConfigManager.settings.netmask, ConfigManager.settings.gateway);
            if (state) {
                ctx.status(200);
            } else {
                ctx.result("Something went wrong while setting network configuration");
                ctx.status(501);
            }
            SocketHandler.sendFullSettings();
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
        } catch (JsonProcessingException ex) {
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

                HashMap<String, String> perViewErrors = new HashMap<>();
                perViewErrors.put("StdDeviationsIntrinsics", pipeManager.calib3dPipe.getStdDeviationsIntrinsics().dump());
                perViewErrors.put("StdDeviationsExtrinsics", pipeManager.calib3dPipe.getStdDeviationsExtrinsics().dump());
                perViewErrors.put("PerViewErrors", pipeManager.calib3dPipe.getPerViewErrors().dump());

                ctx.json(perViewErrors);
                ctx.json(tmp);
                ctx.status(200);
            } else {
                System.err.println("CALFAIL");
                ctx.status(500);
            }
        } else {
            ctx.status(201);
        }
        pipeManager.setCalibrationMode(false);
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
        Platform p = Platform.CurrentPlatform;
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

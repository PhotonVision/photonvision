/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.ShellExec;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.common.util.file.ProgramDirectoryUtilities;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.target.TargetModel;

public class RequestHandler {
    private static final Logger logger = new Logger(RequestHandler.class, LogGroup.WebServer);

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    public static void onSettingUpload(Context ctx) {
        var file = ctx.uploadedFile("zipData");
        if (file != null) {
            // Copy the file from the client to a temporary location
            var tempFilePath =
                    new File(Path.of(System.getProperty("java.io.tmpdir"), file.getFilename()).toString());
            tempFilePath.getParentFile().mkdirs();
            try {
                FileUtils.copyInputStreamToFile(file.getContent(), tempFilePath);
            } catch (IOException e) {
                logger.error("Exception while uploading settings file to temp folder!");
                e.printStackTrace();
                return;
            }

            // Process the file by its extension
            if (file.getExtension().contains("zip")) {
                // .zip files are assumed to be full packages of configuration files
                logger.debug("Processing uploaded settings zip " + file.getFilename());
                ConfigManager.saveUploadedSettingsZip(tempFilePath);

            } else if (file.getFilename().equals(ConfigManager.HW_CFG_FNAME)) {
                // Filenames matching the hardware config .json file are assumed to be
                // hardware config .json's
                logger.debug("Processing uploaded hardware config " + file.getFilename());
                ConfigManager.getInstance().saveUploadedHardwareConfig(tempFilePath.toPath());

            } else if (file.getFilename().equals(ConfigManager.HW_SET_FNAME)) {
                // Filenames matching the hardware settings .json file are assumed to be
                // hardware settings.json's
                logger.debug("Processing uploaded hardware settings" + file.getFilename());
                ConfigManager.getInstance().saveUploadedHardwareSettings(tempFilePath.toPath());

            } else if (file.getFilename().equals(ConfigManager.NET_SET_FNAME)) {
                // Filenames matching the network config .json file are assumed to be
                // network config .json's
                logger.debug("Processing uploaded network config " + file.getFilename());
                ConfigManager.getInstance().saveUploadedNetworkConfig(tempFilePath.toPath());

            } else {
                logger.error(
                        "Couldn't apply provided settings file - did not recognize "
                                + file.getFilename()
                                + " as a supported file.");
                ctx.status(500);
                return;
            }

            ctx.status(200);
            logger.info("Settings uploaded, going down for restart.");
            restartProgram();
        } else {
            logger.error("Couldn't read uploaded file! Ignoring.");
            ctx.status(500);
        }
    }

    public static void onOfflineUpdate(Context ctx) {
        logger.info("Handling offline update .jar upload...");
        var file = ctx.uploadedFile("jarData");
        logger.info("New .jar uploaded successfully.");

        if (file != null) {
            try {
                Path filePath =
                        Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "photonvision.jar");
                File targetFile = new File(filePath.toString());
                var stream = new FileOutputStream(targetFile);

                logger.info(
                        "Streaming user-provided " + file.getFilename() + " into " + targetFile.toString());

                file.getContent().transferTo(stream);
                stream.close();

                ctx.status(200);
                logger.info("New .jar in place, going down for restart...");
                restartProgram();
            } catch (FileNotFoundException e) {
                logger.error(
                        ".jar of this program could not be found. How the heck this program started in the first place is a mystery.");
                ctx.status(500);
            } catch (IOException e) {
                logger.error("Could not overwrite the .jar for this instance of photonvision.");
                ctx.status(500);
            }
        } else {
            logger.error("Couldn't read provided file for new .jar! Ignoring.");
            ctx.status(500);
        }
    }

    @SuppressWarnings("unchecked")
    public static void onGeneralSettings(Context context) throws JsonProcessingException {
        Map<String, Object> map =
                (Map<String, Object>) kObjectMapper.readValue(context.body(), Map.class);

        var networkConfig = NetworkConfig.fromHashMap(map);
        ConfigManager.getInstance().setNetworkSettings(networkConfig);
        ConfigManager.getInstance().requestSave();
        NetworkManager.getInstance().reinitialize();
        NetworkTablesManager.getInstance().setConfig(networkConfig);

        context.status(200);
    }

    @SuppressWarnings("unchecked")
    public static void onCameraSettingsSave(Context context) {
        try {
            var settingsAndIndex = kObjectMapper.readValue(context.body(), Map.class);
            logger.info("Got cam setting json from frontend!\n" + settingsAndIndex.toString());
            var settings = (HashMap<String, Object>) settingsAndIndex.get("settings");
            int index = (Integer) settingsAndIndex.get("index");

            // The only settings we actually care about are FOV
            var fov = Double.parseDouble(settings.get("fov").toString());

            logger.info(String.format("Setting camera %s's fov to %s", index, fov));
            var module = VisionModuleManager.getInstance().getModule(index);
            module.setFov(fov);
            module.saveModule();
        } catch (JsonProcessingException e) {
            logger.error("Got invalid camera setting JSON from frontend!");
            e.printStackTrace();
        }
    }

    public static void onSettingsDownload(Context ctx) {
        logger.info("exporting settings to download...");
        try {
            var zip = ConfigManager.getInstance().getSettingsFolderAsZip();
            var stream = new FileInputStream(zip);
            logger.info("Uploading settings with size " + stream.available());
            ctx.result(stream);
            ctx.contentType("application/zip");
            ctx.header("Content-Disposition: attachment; filename=\"photonvision-settings-export.zip\"");
            ctx.status(200);
        } catch (IOException e) {
            e.printStackTrace();
            ctx.status(501);
            logger.error("Got bad recode from zip to byte");
        }
    }

    private static ShellExec shell = new ShellExec();

    public static void onExportCurrentLogs(Context ctx) {
        if (!Platform.isLinux()) {
            logger.warn("Cannot export journalctl on non-Linux platforms! Ignoring");
            ctx.status(500);
            return;
        }

        try {
            var tempPath = Files.createTempFile("photonvision-journalctl", ".txt");
            shell.executeBashCommand(
                    "journalctl -u photonvision.service > " + tempPath.toAbsolutePath().toString());

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                // Wrote to the temp file! Add it to the ctx
                var stream = new FileInputStream(tempPath.toFile());
                logger.info("Uploading settings with size " + stream.available());
                ctx.result(stream);
                ctx.contentType("application/zip");
                ctx.header("Content-Disposition: attachment; filename=\"photonvision-journalctl.txt\"");
                ctx.status(200);
            } else {
                logger.error("Could not export journactl logs! (exit code != 0)");
                ctx.status(500);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Could not export journactl logs! (IOexception)", e);
            ctx.status(500);
        }
    }

    public static void onCalibrationEnd(Context ctx) {
        logger.info("Calibrating camera! This will take a long time...");
        var index = Integer.parseInt(ctx.body());
        var calData = VisionModuleManager.getInstance().getModule(index).endCalibration();
        if (calData == null) {
            ctx.status(500);
            return;
        }

        ctx.result(String.valueOf(calData.standardDeviation));
        ctx.status(200);
        logger.info("Camera calibrated!");
    }

    public static void restartDevice(Context ctx) {
        ctx.status(HardwareManager.getInstance().restartDevice() ? 200 : 500);
    }

    public static void restartProgram(Context ctx) {
        restartProgram();
    }

    public static void restartProgram() {
        TimedTaskManager.getInstance().addOneShotTask(RequestHandler::restartProgramInternal, 0);
    }

    /**
     * Note that this doesn't actually restart the program itself -- instead, it relies on systemd or
     * an equivalent.
     */
    public static void restartProgramInternal() {
        if (Platform.isLinux()) {
            try {
                new ShellExec().executeBashCommand("systemctl restart photonvision.service");
            } catch (IOException e) {
                logger.error("Could not restart device!", e);
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    public static void importCalibrationFromCalibdb(Context ctx) {
        var file = ctx.body();

        if (file != null) {
            // check if it's a JSON file
            // Load using Jackson
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode actualObj = mapper.readTree(file);

                int cameraIndex = actualObj.get("cameraIndex").asInt();
                String filename = actualObj.get("filename").asText();
                var payload = mapper.readTree(actualObj.get("payload").asText());

                var coeffs = CameraCalibrationCoefficients.parseFromCalibdbJson(payload);

                var uploadCalibrationEvent =
                        new IncomingWebSocketEvent<CameraCalibrationCoefficients>(
                                DataChangeDestination.DCD_ACTIVEMODULE,
                                "calibrationUploaded",
                                coeffs,
                                (Integer) cameraIndex,
                                null);
                DataChangeService.getInstance().publishEvent(uploadCalibrationEvent);

                ctx.status(200);
                logger.info("Calibration added!");
            } catch (Exception e) {
                logger.warn("Could not parse cal metaJSON!");
                e.printStackTrace();
                return;
            }
        } else {
            ctx.status(500);
            return;
        }
    }

    public static void setCameraNickname(Context ctx) {
        try {
            var data = kObjectMapper.readValue(ctx.body(), HashMap.class);
            String name = String.valueOf(data.get("name"));
            int idx = Integer.parseInt(String.valueOf(data.get("cameraIndex")));
            VisionModuleManager.getInstance().getModule(idx).setCameraNickname(name);
            ctx.status(200);
            return;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        ctx.status(500);
    }

    public static void uploadPnpModel(Context ctx) {
        UITargetData data;
        try {
            data = kObjectMapper.readValue(ctx.body(), UITargetData.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            ctx.status(500);
            return;
        }

        VisionModuleManager.getInstance().getModule(data.index).setTargetModel(data.targetModel);
        ctx.status(200);
    }

    public static void sendMetrics(Context ctx) {
        HardwareManager.getInstance().publishMetrics();
        // TimedTaskManager.getInstance().addOneShotTask(() ->
        // RoborioFinder.getInstance().findRios(),
        // 0);
        ctx.status(200);
    }

    public static class UITargetData {
        public int index;
        public TargetModel targetModel;
    }
}

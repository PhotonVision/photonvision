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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
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
import java.util.Optional;
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

public class RequestHandler {
    private static final Logger logger = new Logger(RequestHandler.class, LogGroup.WebServer);

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    private static final ShellExec shell = new ShellExec();

    public static void onSettingsImportRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            return;
        }

        if (!file.getExtension().contains("zip")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'zip'. The uploaded file should be a .zip file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.saveUploadedSettingsZip(tempFilePath.get())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded settings zip");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded zip file");
        }
    }

    public static void onSettingsExportRequest(Context ctx) {
        logger.info("Exporting Settings to ZIP Archive");

        try {
            var zip = ConfigManager.getInstance().getSettingsFolderAsZip();
            var stream = new FileInputStream(zip);
            logger.info("Uploading settings with size " + stream.available());

            ctx.result(stream);
            ctx.contentType("application/zip");
            ctx.header(
                    "Content-Disposition", "attachment; filename=\"photonvision-settings-export.zip\"");

            ctx.status(200);
        } catch (IOException e) {
            logger.error("Unable to export settings archive, bad recode from zip to byte");
            ctx.status(500);
            ctx.result("There was an error while exporting the settings archive");
        }
    }

    public static void onHardwareConfigRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            return;
        }

        if (!file.getExtension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedHardwareConfig(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded hardware config");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded hardware config");
        }
    }

    public static void onHardwareSettingsRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            return;
        }

        if (!file.getExtension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedHardwareSettings(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded hardware config");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded hardware settings");
        }
    }

    public static void onNetworkConfigRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            return;
        }

        if (!file.getExtension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedNetworkConfig(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded hardware config");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded network config");
        }
    }

    public static void onOfflineUpdateRequest(Context ctx) {
        var file = ctx.uploadedFile("jarData");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the new jar is sent at the key 'jarData'");
            return;
        }

        if (!file.getExtension().contains("jar")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'jar'. The uploaded file should be a .jar file.");
            return;
        }

        try {
            Path filePath =
                    Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "photonvision.jar");
            File targetFile = new File(filePath.toString());
            var stream = new FileOutputStream(targetFile);

            file.getContent().transferTo(stream);
            stream.close();

            ctx.status(200);
            restartProgram();
        } catch (FileNotFoundException e) {
            ctx.result("The current program jar file couldn't be found.");
            ctx.status(500);
        } catch (IOException e) {
            ctx.result("Unable to overwrite the existing program with the new program.");
            ctx.status(500);
        }
    }

    public static void onGeneralSettingsRequest(Context ctx) {
        Map<String, Object> map = null;

        try {
            map = (Map<String, Object>) kObjectMapper.readValue(ctx.body(), Map.class);
        } catch (JsonProcessingException e) {

            throw new RuntimeException(e);
        }

        var networkConfig = NetworkConfig.fromHashMap(map);

        if (networkConfig.isEmpty()) {
            ctx.status(400);
            ctx.result("The provided general settings were malformed");
            return;
        }

        ConfigManager.getInstance().setNetworkSettings(networkConfig.get());
        ConfigManager.getInstance().requestSave();

        NetworkManager.getInstance().reinitialize();

        NetworkTablesManager.getInstance().setConfig(networkConfig.get());

        ctx.status(200);
        ctx.result("Successfully saved general settings");
    }

    public static void onCameraSettingsRequest(Context ctx) {
        try {
            var settingsAndIndex = kObjectMapper.readValue(ctx.body(), Map.class);

            var settings = (HashMap<String, Object>) settingsAndIndex.get("settings");
            int index = (Integer) settingsAndIndex.get("index");

            // The only settings we actually care about are FOV
            var fov = Double.parseDouble(settings.get("fov").toString());

            var module = VisionModuleManager.getInstance().getModule(index);
            module.setFov(fov);
            module.saveModule();

            ctx.status(200);
            ctx.result("Successfully saved camera settings");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result("The provided camera settings were malformed");
        }
    }

    public static void onLogExportRequest(Context ctx) {
        if (!Platform.isLinux()) {
            ctx.status(405);
            ctx.result("Logs can only be exported on a Linux platform");
            return;
        }

        try {
            var tempPath = Files.createTempFile("photonvision-journalctl", ".txt");
            shell.executeBashCommand("journalctl -u photonvision.service > " + tempPath.toAbsolutePath());

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                // Wrote to the temp file! Add it to the ctx
                var stream = new FileInputStream(tempPath.toFile());
                logger.info("Uploading settings with size " + stream.available());
                ctx.result(stream);
                ctx.contentType("text/plain");
                ctx.header("Content-Disposition", "attachment; filename=\"photonvision-journalctl.txt\"");

                ctx.status(200);
            } else {
                ctx.status(500);
                ctx.result("The journalctl service was unable to export logs");
            }
        } catch (IOException e) {
            logger.error("Could not export journactl logs!", e);
            ctx.status(500);
            ctx.result("There was an error while exporting journactl logs");
        }
    }

    public static void onCalibrationEndRequest(Context ctx) {
        logger.info("Calibrating camera! This will take a long time...");

        int index;

        try {
            index = (int) kObjectMapper.readValue(ctx.body(), HashMap.class).get("idx");

            var calData = VisionModuleManager.getInstance().getModule(index).endCalibration();
            if (calData == null) {
                ctx.result("The calibration process failed");
                ctx.status(500);
                return;
            }

            ctx.result(String.valueOf(calData.standardDeviation));
            ctx.status(200);

            logger.info("Camera calibrated!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result(
                    "The 'idx' field was not found in the request. Please make sure the index of the vision module is specified with the 'idx' key.");
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("There was an error while ending calibration");
        }
    }

    public static void onCalibrationImportRequest(Context ctx) {
        var data = ctx.body();

        try {
            ObjectMapper mapper = new ObjectMapper();

            var actualObj = mapper.readTree(data);

            int cameraIndex = actualObj.get("cameraIndex").asInt();
            var payload = mapper.readTree(actualObj.get("payload").asText());
            var coeffs = CameraCalibrationCoefficients.parseFromCalibdbJson(payload);

            var uploadCalibrationEvent =
                    new IncomingWebSocketEvent<>(
                            DataChangeDestination.DCD_ACTIVEMODULE,
                            "calibrationUploaded",
                            coeffs,
                            cameraIndex,
                            null);
            DataChangeService.getInstance().publishEvent(uploadCalibrationEvent);

            ctx.status(200);
            logger.info("Calibration added!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result("The provided nickname data was malformed");
        }
    }

    public static void onProgramRestartRequest(Context ctx) {
        // TODO, check if this was successful or not
        restartProgram();
    }

    public static void onDeviceRestartRequest(Context ctx) {
        ctx.status(HardwareManager.getInstance().restartDevice() ? 204 : 500);
    }

    public static void onCameraNicknameChangeRequest(Context ctx) {
        try {
            var data = kObjectMapper.readValue(ctx.body(), HashMap.class);

            String name = String.valueOf(data.get("name"));
            int idx = Integer.parseInt(String.valueOf(data.get("cameraIndex")));

            VisionModuleManager.getInstance().getModule(idx).setCameraNickname(name);
            ctx.status(200);
            ctx.result("Successfully changed the camera name to: " + name);
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result("The provided nickname data was malformed");
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("An error occurred while changing the camera's nickname");
        }
    }

    public static void onMetricsPublishRequest(Context ctx) {
        HardwareManager.getInstance().publishMetrics();
        ctx.status(204);
    }

    /**
     * Create a temporary file using the UploadedFile from Javalin.
     *
     * @param file the uploaded file.
     * @return if the temporary file was successfully created.
     */
    private static Optional<File> handleTempFileCreation(UploadedFile file) {
        var tempFilePath =
                new File(Path.of(System.getProperty("java.io.tmpdir"), file.getFilename()).toString());

        boolean createFile = tempFilePath.getParentFile().mkdirs();

        if (!createFile) return Optional.empty();

        try {
            FileUtils.copyInputStreamToFile(file.getContent(), tempFilePath);
        } catch (IOException e) {
            logger.error(
                    "There was an error while uploading " + file.getFilename() + " to the temp folder!");
            return Optional.empty();
        }

        return Optional.of(tempFilePath);
    }

    /**
     * Restart the running program. Note that this doesn't actually restart the program itself,
     * instead, it relies on systemd or an equivalent.
     */
    private static void restartProgram() {
        TimedTaskManager.getInstance()
                .addOneShotTask(
                        () -> {
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
                        },
                        0);
    }
}

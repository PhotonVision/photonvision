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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import javax.imageio.ImageIO;
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
    // Treat all 2XX calls as "INFO"
    // Treat all 4XX calls as "ERROR"
    // Treat all 5XX calls as "ERROR"

    private static final Logger logger = new Logger(RequestHandler.class, LogGroup.WebServer);

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    public static void onSettingsImportRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the settings zip is sent at the key 'data'");
            return;
        }

        if (!file.extension().contains("zip")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'zip'. The uploaded file should be a .zip file.");
            logger.error(
                    "The uploaded file was not of type 'zip'. The uploaded file should be a .zip file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            logger.error("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.saveUploadedSettingsZip(tempFilePath.get())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded settings zip");
            logger.info("Successfully saved the uploaded settings zip");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded zip file");
            logger.error("There was an error while saving the uploaded zip file");
        }
    }

    public static void onSettingsExportRequest(Context ctx) {
        logger.info("Exporting Settings to ZIP Archive");

        try {
            var zip = ConfigManager.getInstance().getSettingsFolderAsZip();
            var stream = new FileInputStream(zip);
            logger.info("Uploading settings with size " + stream.available());

            ctx.contentType("application/zip");
            ctx.header(
                    "Content-Disposition", "attachment; filename=\"photonvision-settings-export.zip\"");

            ctx.result(stream);
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
                    "No File was sent with the request. Make sure that the hardware config json is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the hardware config json is sent at the key 'data'");
            return;
        }

        if (!file.extension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            logger.error(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            logger.error("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedHardwareConfig(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded hardware config");
            logger.info("Successfully saved the uploaded hardware config");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded hardware config");
            logger.error("There was an error while saving the uploaded hardware config");
        }
    }

    public static void onHardwareSettingsRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the hardware settings json is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the hardware settings json is sent at the key 'data'");
            return;
        }

        if (!file.extension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            logger.error(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            logger.error("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedHardwareSettings(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded hardware settings");
            logger.info("Successfully saved the uploaded hardware settings");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded hardware settings");
            logger.error("There was an error while saving the uploaded hardware settings");
        }
    }

    public static void onNetworkConfigRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the network config json is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the network config json is sent at the key 'data'");
            return;
        }

        if (!file.extension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            logger.error(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            logger.error("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedNetworkConfig(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded network config");
            logger.info("Successfully saved the uploaded network config");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded network config");
            logger.error("There was an error while saving the uploaded network config");
        }
    }

    public static void onAprilTagFieldLayoutRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the field layout json is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the field layout json is sent at the key 'data'");
            return;
        }

        if (!file.extension().contains("json")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            logger.error(
                    "The uploaded file was not of type 'json'. The uploaded file should be a .json file.");
            return;
        }

        // Create a temp file
        var tempFilePath = handleTempFileCreation(file);

        if (tempFilePath.isEmpty()) {
            ctx.status(500);
            ctx.result("There was an error while creating a temporary copy of the file");
            logger.error("There was an error while creating a temporary copy of the file");
            return;
        }

        if (ConfigManager.getInstance().saveUploadedAprilTagFieldLayout(tempFilePath.get().toPath())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded AprilTagFieldLayout");
            logger.info("Successfully saved the uploaded AprilTagFieldLayout");
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded AprilTagFieldLayout");
            logger.error("There was an error while saving the uploaded AprilTagFieldLayout");
        }
    }

    public static void onOfflineUpdateRequest(Context ctx) {
        var file = ctx.uploadedFile("jarData");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the new jar is sent at the key 'jarData'");
            logger.error(
                    "No File was sent with the request. Make sure that the new jar is sent at the key 'jarData'");
            return;
        }

        if (!file.extension().contains("jar")) {
            ctx.status(400);
            ctx.result(
                    "The uploaded file was not of type 'jar'. The uploaded file should be a .jar file.");
            logger.error(
                    "The uploaded file was not of type 'jar'. The uploaded file should be a .jar file.");
            return;
        }

        try {
            Path filePath =
                    Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "photonvision.jar");
            File targetFile = new File(filePath.toString());
            var stream = new FileOutputStream(targetFile);

            file.content().transferTo(stream);
            stream.close();

            ctx.status(200);
            ctx.result(
                    "Offline update successfully complete. PhotonVision will restart in the background.");
            logger.info(
                    "Offline update successfully complete. PhotonVision will restart in the background.");
            restartProgram();
        } catch (FileNotFoundException e) {
            ctx.result("The current program jar file couldn't be found.");
            ctx.status(500);
            logger.error("The current program jar file couldn't be found.", e);
        } catch (IOException e) {
            ctx.result("Unable to overwrite the existing program with the new program.");
            ctx.status(500);
            logger.error("Unable to overwrite the existing program with the new program.", e);
        }
    }

    public static void onGeneralSettingsRequest(Context ctx) {
        NetworkConfig config;
        try {
            config = kObjectMapper.readValue(ctx.body(), NetworkConfig.class);

            ctx.status(200);
            ctx.result("Successfully saved general settings");
            logger.info("Successfully saved general settings");
        } catch (JsonProcessingException e) {
            // If the settings can't be parsed, use the default network settings
            config = new NetworkConfig();

            ctx.status(400);
            ctx.result("The provided general settings were malformed");
            logger.error("The provided general settings were malformed", e);
        }

        ConfigManager.getInstance().setNetworkSettings(config);
        ConfigManager.getInstance().requestSave();

        NetworkManager.getInstance().reinitialize();

        NetworkTablesManager.getInstance().setConfig(config);
    }

    public static void onCameraSettingsRequest(Context ctx) {
        try {
            var data = kObjectMapper.readTree(ctx.body());

            int index = data.get("index").asInt();
            double fov = data.get("settings").get("fov").asDouble();

            var module = VisionModuleManager.getInstance().getModule(index);
            module.setFov(fov);

            module.saveModule();

            ctx.status(200);
            ctx.result("Successfully saved camera settings");
            logger.info("Successfully saved camera settings");
        } catch (JsonProcessingException | NullPointerException e) {
            ctx.status(400);
            ctx.result("The provided camera settings were malformed");
            logger.error("The provided camera settings were malformed", e);
        }
    }

    public static void onLogExportRequest(Context ctx) {
        if (!Platform.isLinux()) {
            ctx.status(405);
            ctx.result("Logs can only be exported on a Linux platform");
            // INFO only log because this isn't ERROR worthy
            logger.info("Logs can only be exported on a Linux platform");
            return;
        }

        try {
            ShellExec shell = new ShellExec();
            var tempPath = Files.createTempFile("photonvision-journalctl", ".txt");
            shell.executeBashCommand("journalctl -u photonvision.service > " + tempPath.toAbsolutePath());

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                // Wrote to the temp file! Add it to the ctx
                var stream = new FileInputStream(tempPath.toFile());
                ctx.contentType("text/plain");
                ctx.header("Content-Disposition", "attachment; filename=\"photonvision-journalctl.txt\"");
                ctx.status(200);
                ctx.result(stream);
                logger.info("Uploading settings with size " + stream.available());
            } else {
                ctx.status(500);
                ctx.result("The journalctl service was unable to export logs");
                logger.error("The journalctl service was unable to export logs");
            }
        } catch (IOException e) {
            ctx.status(500);
            ctx.result("There was an error while exporting journactl logs");
            logger.error("There was an error while exporting journactl logs", e);
        }
    }

    public static void onCalibrationEndRequest(Context ctx) {
        logger.info("Calibrating camera! This will take a long time...");

        int index;

        try {
            index = kObjectMapper.readTree(ctx.body()).get("index").asInt();

            var calData = VisionModuleManager.getInstance().getModule(index).endCalibration();
            if (calData == null) {
                ctx.result("The calibration process failed");
                ctx.status(500);
                logger.error(
                        "The calibration process failed. Calibration data for module at index ("
                                + index
                                + ") was null");
                return;
            }

            ctx.result("Camera calibration successfully completed!");
            ctx.status(200);
            logger.info("Camera calibration successfully completed!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result(
                    "The 'index' field was not found in the request. Please make sure the index of the vision module is specified with the 'index' key.");
            logger.error(
                    "The 'index' field was not found in the request. Please make sure the index of the vision module is specified with the 'index' key.",
                    e);
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("There was an error while ending calibration");
            logger.error("There was an error while ending calibration", e);
        }
    }

    public static void onCalibrationImportRequest(Context ctx) {
        var data = ctx.body();

        try {
            var actualObj = kObjectMapper.readTree(data);

            int cameraIndex = actualObj.get("cameraIndex").asInt();
            var payload = kObjectMapper.readTree(actualObj.get("payload").asText());
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
            ctx.result("Calibration imported successfully from CalibDB data!");
            logger.info("Calibration imported successfully from CalibDB data!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result(
                    "The Provided CalibDB data is malformed and cannot be parsed for the required fields.");
            logger.error(
                    "The Provided CalibDB data is malformed and cannot be parsed for the required fields.",
                    e);
        }
    }

    public static void onProgramRestartRequest(Context ctx) {
        // TODO, check if this was successful or not
        ctx.status(204);
        restartProgram();
    }

    public static void onDeviceRestartRequest(Context ctx) {
        ctx.status(HardwareManager.getInstance().restartDevice() ? 204 : 500);
    }

    public static void onCameraNicknameChangeRequest(Context ctx) {
        try {
            var data = kObjectMapper.readTree(ctx.body());

            String name = data.get("name").asText();
            int idx = data.get("cameraIndex").asInt();

            VisionModuleManager.getInstance().getModule(idx).setCameraNickname(name);
            ctx.status(200);
            ctx.result("Successfully changed the camera name to: " + name);
            logger.info("Successfully changed the camera name to: " + name);
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result("The provided nickname data was malformed");
            logger.error("The provided nickname data was malformed", e);

        } catch (Exception e) {
            ctx.status(500);
            ctx.result("An error occurred while changing the camera's nickname");
            logger.error("An error occurred while changing the camera's nickname", e);
        }
    }

    public static void onMetricsPublishRequest(Context ctx) {
        HardwareManager.getInstance().publishMetrics();
        ctx.status(204);
    }

    public static void onImageSnapshotsRequest(Context ctx) {
        var snapshots = new ArrayList<HashMap<String, Object>>();
        var cameraDirs = ConfigManager.getInstance().getImageSavePath().toFile().listFiles();

        if (cameraDirs != null) {
            try {
                for(File cameraDir : cameraDirs) {
                    var cameraSnapshots = cameraDir.listFiles();
                    if(cameraSnapshots == null) continue;

                    String cameraUniqueName = cameraDir.getName();

                    for(File snapshot : cameraSnapshots) {
                        var snapshotData = new HashMap<String, Object>();

                        var bufferedImage = ImageIO.read(snapshot);
                        var buffer = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", buffer);
                        byte[] data = buffer.toByteArray();

                        snapshotData.put("snapshotName", snapshot.getName());
                        snapshotData.put("cameraUniqueName", cameraUniqueName);
                        snapshotData.put("snapshotData", data);

                        snapshots.add(snapshotData);
                    }
                }
            } catch (IOException e) {
                ctx.status(500);
                ctx.result("Unable to read saved images");
            }
        }

        ctx.status(200);
        ctx.json(snapshots);
    }

    /**
     * Create a temporary file using the UploadedFile from Javalin.
     *
     * @param file the uploaded file.
     * @return Temporary file. Empty if the temporary file was unable to be created.
     */
    private static Optional<File> handleTempFileCreation(UploadedFile file) {
        var tempFilePath =
                new File(Path.of(System.getProperty("java.io.tmpdir"), file.filename()).toString());
        boolean makeDirsRes = tempFilePath.getParentFile().mkdirs();

        if (!makeDirsRes && !(tempFilePath.getParentFile().exists())) {
            logger.error(
                    "There was an error while creating "
                            + tempFilePath.getAbsolutePath()
                            + "! Exists: "
                            + tempFilePath.getParentFile().exists());
            return Optional.empty();
        }

        try {
            FileUtils.copyInputStreamToFile(file.content(), tempFilePath);
        } catch (IOException e) {
            logger.error(
                    "There was an error while copying "
                            + file.filename()
                            + " to the temp file "
                            + tempFilePath.getAbsolutePath());
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

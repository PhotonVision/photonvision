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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.dataflow.websocket.UIPhotonConfiguration;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.ShellExec;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.common.util.file.ProgramDirectoryUtilities;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.objects.ObjectDetector;
import org.photonvision.vision.objects.RknnModel;
import org.photonvision.vision.objects.RubikModel;
import org.photonvision.vision.processes.VisionSourceManager;
import org.zeroturnaround.zip.ZipUtil;

public class RequestHandler {
    // Treat all 2XX calls as "INFO"
    // Treat all 4XX calls as "ERROR"
    // Treat all 5XX calls as "ERROR"

    private static final Logger logger = new Logger(RequestHandler.class, LogGroup.WebServer);

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    private record CommonCameraUniqueName(String cameraUniqueName) {}

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

        ConfigManager.getInstance().setWriteTaskEnabled(false);
        ConfigManager.getInstance().disableFlushOnShutdown();
        // We want to delete the -whole- zip file, so we need to teardown loggers for
        // now
        logger.info("Writing new settings zip (logs may be truncated)...");
        Logger.closeAllLoggers();
        if (ConfigManager.saveUploadedSettingsZip(tempFilePath.get())) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded settings zip, rebooting...");
            restartProgram();
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
            ctx.result("Successfully saved the uploaded hardware config, rebooting...");
            logger.info("Successfully saved the uploaded hardware config, rebooting...");
            restartProgram();
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
            ctx.result("Successfully saved the uploaded hardware settings, rebooting...");
            logger.info("Successfully saved the uploaded hardware settings, rebooting...");
            restartProgram();
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
            ctx.result("Successfully saved the uploaded network config, rebooting...");
            logger.info("Successfully saved the uploaded network config, rebooting...");
            restartProgram();
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
            ctx.result("Successfully saved the uploaded AprilTagFieldLayout, rebooting...");
            logger.info("Successfully saved the uploaded AprilTagFieldLayout, rebooting...");
            restartProgram();
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

        Path targetPath =
                Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "photonvision.jar");
        try (InputStream fileSteam = file.content()) {
            Files.copy(fileSteam, targetPath, StandardCopyOption.REPLACE_EXISTING);

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
            config = kObjectMapper.readValue(ctx.bodyInputStream(), NetworkConfig.class);

            ctx.status(200);
            ctx.result("Successfully saved general settings");
            logger.info("Successfully saved general settings");
        } catch (IOException e) {
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

    private record CameraSettingsRequest(
            double fov, HashMap<CameraQuirk, Boolean> quirksToChange, String cameraUniqueName) {}

    public static void onCameraSettingsRequest(Context ctx) {
        try {
            CameraSettingsRequest request =
                    kObjectMapper.readValue(ctx.body(), CameraSettingsRequest.class);
            // Extract the settings from the request
            double fov = request.fov;
            HashMap<CameraQuirk, Boolean> quirksToChange = request.quirksToChange;
            String cameraUniqueName = request.cameraUniqueName;

            if (cameraUniqueName == null || cameraUniqueName.isEmpty()) {
                ctx.status(400).result("cameraUniqueName is required");
                logger.error("cameraUniqueName is missing in the request");
                return;
            }

            logger.info("Changing camera FOV to: " + fov);

            logger.info("Changing quirks to: " + quirksToChange.toString());

            var module = VisionSourceManager.getInstance().vmm.getModule(cameraUniqueName);

            module.setFov(fov);
            module.changeCameraQuirks(quirksToChange);
            module.saveModule();

            ctx.status(200).result("Camera settings updated successfully");
        } catch (Exception e) {
            logger.error("Failed to process camera settings request", e);
            ctx.status(500).result("Failed to process camera settings request");
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
            var tempPath2 = Files.createTempFile("photonvision-kernelogs", ".txt");
            // In the command below:
            // dmesg = output all kernel logs since current boot
            // cat /var/log/kern.log = output all kernel logs since first boot
            shell.executeBashCommand(
                    "journalctl -a --output cat -u photonvision.service | sed -E 's/\\x1B\\[(0|30|31|32|33|34|35|36|37)m//g' >"
                            + tempPath.toAbsolutePath()
                            + " && dmesg > "
                            + tempPath2.toAbsolutePath());

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                // Wrote to the temp file! Zip and yeet it to the client

                var out = Files.createTempFile("photonvision-logs", "zip").toFile();

                try {
                    ZipUtil.packEntries(new File[] {tempPath.toFile(), tempPath2.toFile()}, out);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                var stream = new FileInputStream(out);
                ctx.contentType("application/zip");
                ctx.header("Content-Disposition", "attachment; filename=\"photonvision-logs.zip\"");
                ctx.result(stream);
                ctx.status(200);
                logger.info("Outputting log ZIP with size " + stream.available());
            } else {
                ctx.status(500);
                ctx.result("The journalctl service was unable to export logs");
                logger.error("The journalctl service was unable to export logs");
            }
        } catch (IOException e) {
            ctx.status(500);
            ctx.result("There was an error while exporting journalctl logs");
            logger.error("There was an error while exporting journalctl logs", e);
        }
    }

    public static void onCalibrationEndRequest(Context ctx) {
        logger.info("Calibrating camera! This will take a long time...");

        try {
            CommonCameraUniqueName request =
                    kObjectMapper.readValue(ctx.body(), CommonCameraUniqueName.class);

            var calData =
                    VisionSourceManager.getInstance()
                            .vmm
                            .getModule(request.cameraUniqueName)
                            .endCalibration();
            if (calData == null) {
                ctx.result("The calibration process failed");
                ctx.status(500);
                logger.error(
                        "The calibration process failed. Calibration data for module at cameraUniqueName ("
                                + request.cameraUniqueName
                                + ") was null");
                return;
            }

            ctx.result("Camera calibration successfully completed!");
            ctx.status(200);
            logger.info("Camera calibration successfully completed!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result(
                    "The 'cameraUniqueName' field was not found in the request. Please make sure the cameraUniqueName of the vision module is specified with the 'cameraUniqueName' key.");
            logger.error(
                    "The 'cameraUniqueName' field was not found in the request. Please make sure the cameraUniqueName of the vision module is specified with the 'cameraUniqueName' key.",
                    e);
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("There was an error while ending calibration");
            logger.error("There was an error while ending calibration", e);
        }
    }

    private record DataCalibrationImportRequest(
            String cameraUniqueName, CameraCalibrationCoefficients calibration) {}

    public static void onDataCalibrationImportRequest(Context ctx) {
        try {
            // DataCalibrationImportRequest request =
            //         kObjectMapper.readValue(ctx.body(), DataCalibrationImportRequest.class);
            DataCalibrationImportRequest request =
                    kObjectMapper.readValue(ctx.req().getInputStream(), DataCalibrationImportRequest.class);

            var uploadCalibrationEvent =
                    new IncomingWebSocketEvent<>(
                            DataChangeDestination.DCD_ACTIVEMODULE,
                            "calibrationUploaded",
                            request.calibration,
                            request.cameraUniqueName,
                            null);
            DataChangeService.getInstance().publishEvent(uploadCalibrationEvent);

            ctx.status(200);
            ctx.result("Calibration imported successfully from imported data!");
            logger.info("Calibration imported successfully from imported data!");
        } catch (JsonProcessingException e) {
            ctx.status(400);
            ctx.result("The provided calibration data was malformed");
            logger.error("The provided calibration data was malformed", e);

        } catch (Exception e) {
            ctx.status(500);
            ctx.result("An error occurred while uploading calibration data");
            logger.error("An error occurred while uploading calibration data", e);
        }
    }

    public static void onProgramRestartRequest(Context ctx) {
        // TODO, check if this was successful or not
        ctx.status(204);
        restartProgram();
    }

    public static void onImportObjectDetectionModelRequest(Context ctx) {
        try {
            // Retrieve the uploaded files
            var modelFile = ctx.uploadedFile("modelFile");

            // Strip any whitespaces on either side of the commas
            LinkedList<String> labels = new LinkedList<>();
            String rawLabels = ctx.formParam("labels");
            if (rawLabels != null) {
                for (String label : rawLabels.split(",")) {
                    labels.add(label.trim());
                }
            }
            int width = Integer.parseInt(ctx.formParam("width"));
            int height = Integer.parseInt(ctx.formParam("height"));
            NeuralNetworkModelManager.Version version =
                    switch (ctx.formParam("version").toString()) {
                        case "YOLOv5" -> NeuralNetworkModelManager.Version.YOLOV5;
                        case "YOLOv8" -> NeuralNetworkModelManager.Version.YOLOV8;
                        case "YOLO11" -> NeuralNetworkModelManager.Version.YOLOV11;
                            // Add more versions as necessary for new models
                        default -> {
                            ctx.status(400);
                            ctx.result("The provided version was not valid");
                            logger.error("The provided version was not valid");
                            yield null;
                        }
                    };

            if (modelFile == null) {
                ctx.status(400);
                ctx.result(
                        "No File was sent with the request. Make sure that the model file is sent at the key 'modelFile'");
                logger.error(
                        "No File was sent with the request. Make sure that the model file is sent at the key 'modelFile'");
                return;
            }

            if (labels == null || labels.isEmpty()) {
                ctx.status(400);
                ctx.result("The provided labels were malformed");
                logger.error("The provided labels were malformed");
                return;
            }

            if (width < 0 || height < 0 || width != Math.floor(width) || height != Math.floor(height)) {
                ctx.status(400);
                ctx.result(
                        "The provided width and height were malformed. They must be integers greater than one.");
                logger.error(
                        "The provided width and height were malformed.  They must be integers greater than one.");
                return;
            }

            NeuralNetworkModelManager.Family family;

            switch (Platform.getCurrentPlatform()) {
                case LINUX_QCS6490:
                    family = NeuralNetworkModelManager.Family.RUBIK;
                    break;
                case LINUX_RK3588_64:
                    family = NeuralNetworkModelManager.Family.RKNN;
                    break;
                default:
                    ctx.status(400);
                    ctx.result("The current platform does not support object detection models");
                    logger.error("The current platform does not support object detection models");
                    return;
            }

            // If adding additional platforms, check platform matches
            if (!modelFile.extension().contains(family.extension())) {
                ctx.status(400);
                ctx.result(
                        "The uploaded file was not of type '"
                                + family.extension()
                                + "'. The uploaded file should be a ."
                                + family.extension()
                                + " file.");
                logger.error(
                        "The uploaded file was not of type '"
                                + family.extension()
                                + "'. The uploaded file should be a ."
                                + family.extension()
                                + " file.");
                return;
            }

            Path modelPath =
                    Paths.get(
                            ConfigManager.getInstance().getModelsDirectory().toString(), modelFile.filename());

            if (modelPath.toFile().exists()) {
                ctx.status(400);
                ctx.result(
                        "The model file already exists. Please delete the existing model file before uploading a new one.");
                logger.error(
                        "The model file already exists. Please delete the existing model file before uploading a new one.");
                return;
            }

            try (InputStream modelFileStream = modelFile.content()) {
                Files.copy(modelFileStream, modelPath, StandardCopyOption.REPLACE_EXISTING);
            }

            int idx = modelFile.filename().lastIndexOf('.');
            String nickname = modelFile.filename().substring(0, idx);

            ModelProperties modelProperties =
                    new ModelProperties(modelPath, nickname, labels, width, height, family, version);

            ObjectDetector objDetector = null;

            try {
                objDetector =
                        switch (family) {
                            case RUBIK -> new RubikModel(modelProperties).load();
                            case RKNN -> new RknnModel(modelProperties).load();
                        };
            } catch (RuntimeException e) {
                ctx.status(400);
                ctx.result("Failed to load object detection model: " + e.getMessage());

                try {
                    Files.deleteIfExists(modelPath);
                } catch (IOException ex) {
                    e.addSuppressed(ex);
                }

                logger.error("Failed to load object detection model", e);
                return;
            } finally {
                // this finally block will run regardless of what happens in try/catch
                // please see https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html
                // for a summary on how finally works
                if (objDetector != null) {
                    objDetector.release();
                }
            }

            ConfigManager.getInstance()
                    .getConfig()
                    .neuralNetworkPropertyManager()
                    .addModelProperties(modelProperties);

            logger.debug(
                    ConfigManager.getInstance().getConfig().neuralNetworkPropertyManager().toString());

            NeuralNetworkModelManager.getInstance().discoverModels();

            ctx.status(200).result("Successfully uploaded object detection model");
        } catch (Exception e) {
            ctx.status(500).result("Error processing files: " + e.getMessage());
            logger.error("Error processing new object detection model", e);
        }

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));
    }

    public static void onExportObjectDetectionModelsRequest(Context ctx) {
        logger.info("Exporting Object Detection Models to ZIP Archive");

        try {
            var zip = ConfigManager.getInstance().getObjectDetectionExportAsZip();
            var stream = new FileInputStream(zip);
            logger.info("Uploading object detection models with size " + stream.available());

            ctx.contentType("application/zip");
            ctx.header(
                    "Content-Disposition",
                    "attachment; filename=\"photonvision-object-detection-models-export.zip\"");

            ctx.result(stream);
            ctx.status(200);
        } catch (IOException e) {
            logger.error("Unable to export object detection models archive, bad recode from zip to byte");
            ctx.status(500);
            ctx.result("There was an error while exporting the object detection models archive");
        }
    }

    public static void onExportIndividualObjectDetectionModelRequest(Context ctx) {
        logger.info("Exporting Individual Object Detection Model");

        try {
            String modelPath = ctx.queryParam("modelPath");

            if (modelPath == null || modelPath.isEmpty()) {
                ctx.status(400);
                ctx.result("The provided model path was malformed");
                logger.error("The provided model path was malformed");
                return;
            }

            File modelFile = NeuralNetworkModelManager.getInstance().exportSingleModel(modelPath);

            var stream = new FileInputStream(modelFile);
            logger.info("Uploading object detection model with size " + stream.available());

            ctx.contentType("application/octet-stream");
            ctx.header("Content-Disposition", "attachment; filename=" + modelFile.getName());

            ctx.result(stream);
            ctx.status(200);
        } catch (IOException e) {
            logger.error("Unable to export object detection model, " + e);
            ctx.status(500);
            ctx.result("There was an error while exporting the object detection model");
        }
    }

    public static void onBulkImportObjectDetectionModelRequest(Context ctx) {
        var file = ctx.uploadedFile("data");

        if (file == null) {
            ctx.status(400);
            ctx.result(
                    "No File was sent with the request. Make sure that the object detection zip is sent at the key 'data'");
            logger.error(
                    "No File was sent with the request. Make sure that the object detection zip file is sent at the key 'data'");
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

        Path tempDir = null;
        // Extract .rknn files from zip and move to models directory
        try {
            tempDir = Files.createTempDirectory("photonvision-od-models");
            ZipUtil.unpack(tempFilePath.get(), tempDir.toFile());

            Path targetModelsDir = ConfigManager.getInstance().getModelsDirectory().toPath();

            // Copy all files from the source models directory to the target models
            // directory
            try (var stream = Files.list(tempDir)) {
                for (Path modelFile : stream.toList()) {
                    if (Files.isRegularFile(modelFile)
                            && !modelFile.getFileName().toString().endsWith(".json")) {
                        logger.debug("Copying model file: " + modelFile.getFileName());
                        Files.copy(
                                modelFile,
                                Path.of(targetModelsDir.toString(), modelFile.getFileName().toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            logger.info("Successfully copied models from " + tempDir + " to " + targetModelsDir);

        } catch (Exception e) {
            ctx.status(500);
            ctx.result("There was an error while extracting and coyping the object detection models");
            logger.error(
                    "There was an error while extracting and copying the object detection models", e);
            return;
        }

        if (ConfigManager.getInstance()
                .saveUploadedNeuralNetworkProperties(
                        Path.of(tempDir.toString(), "photonvision-object-detection-models.json"))) {
            ctx.status(200);
            ctx.result("Successfully saved the uploaded object detection models, rebooting...");
            logger.info("Successfully saved the uploaded object detection models, rebooting...");
            restartProgram();
        } else {
            ctx.status(500);
            ctx.result("There was an error while saving the uploaded object detection models");
            logger.error("There was an error while saving the uploaded object detection models");
        }
    }

    private record DeleteObjectDetectionModelRequest(String modelPath) {}

    public static void onDeleteObjectDetectionModelRequest(Context ctx) {
        logger.info("Deleting object detection model");
        Path modelPath;

        try {
            DeleteObjectDetectionModelRequest request =
                    JacksonUtils.deserialize(ctx.body(), DeleteObjectDetectionModelRequest.class);

            modelPath = Path.of(request.modelPath.substring(7));

            if (modelPath == null) {
                ctx.status(400);
                ctx.result("The provided model path was malformed");
                logger.error("The provided model path was malformed");
                return;
            }

            if (!modelPath.toFile().exists()) {
                ctx.status(400);
                ctx.result("The provided model path does not exist");
                logger.error("The provided model path does not exist");
                return;
            }

            if (!modelPath.toFile().delete()) {
                ctx.status(500);
                ctx.result("Unable to delete the model file");
                logger.error("Unable to delete the model file");
                return;
            }

            if (!ConfigManager.getInstance()
                    .getConfig()
                    .neuralNetworkPropertyManager()
                    .removeModel(modelPath)) {
                ctx.status(400);
                ctx.result("The model's information was not found in the config");
                logger.error("The model's information was not found in the config");
                return;
            }

            NeuralNetworkModelManager.getInstance().discoverModels();

            ctx.status(200).result("Successfully deleted object detection model");

        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Error deleting object detection model: " + e.getMessage());
            logger.error("Error deleting object detection model", e);
        }

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));
    }

    private record RenameObjectDetectionModelRequest(String modelPath, String newName) {}

    public static void onRenameObjectDetectionModelRequest(Context ctx) {
        try {
            RenameObjectDetectionModelRequest request =
                    JacksonUtils.deserialize(ctx.body(), RenameObjectDetectionModelRequest.class);

            Path modelPath = Path.of(request.modelPath);

            if (modelPath == null) {
                ctx.status(400);
                ctx.result("The provided model path was malformed");
                logger.error("The provided model path was malformed");
                return;
            }

            if (!modelPath.toFile().exists()) {
                ctx.status(400);
                ctx.result("The provided model path does not exist");
                logger.error("The model path: " + modelPath + " does not exist");
                return;
            }

            if (request.newName == null || request.newName.isEmpty()) {
                ctx.status(400);
                ctx.result("The provided new name was malformed");
                logger.error("The provided new name was malformed");
                return;
            }

            if (!ConfigManager.getInstance()
                    .getConfig()
                    .neuralNetworkPropertyManager()
                    .renameModel(modelPath, request.newName)) {
                ctx.status(400);
                ctx.result("The model's information was not found in the config");
                logger.error("The model's information was not found in the config");
                return;
            }

            NeuralNetworkModelManager.getInstance().discoverModels();
            ctx.status(200).result("Successfully renamed object detection model");
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Error renaming object detection model: " + e.getMessage());
            logger.error("Error renaming object detection model", e);
            return;
        }
    }

    public static void onNukeObjectDetectionModelsRequest(Context ctx) {
        logger.info("Attempting to clear object detection models");
        try {
            NeuralNetworkModelManager.getInstance().clearModels();
            NeuralNetworkModelManager.getInstance().extractModels();
            ctx.status(200).result("Successfully cleared and reset object detection models");
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Error clearing object detection models: " + e.getMessage());
            logger.error("Error clearing object detection models", e);
        }
    }

    public static void onDeviceRestartRequest(Context ctx) {
        ctx.status(HardwareManager.getInstance().restartDevice() ? 204 : 500);
    }

    private record CameraNicknameChangeRequest(String name, String cameraUniqueName) {}

    public static void onCameraNicknameChangeRequest(Context ctx) {
        try {
            CameraNicknameChangeRequest request =
                    kObjectMapper.readValue(ctx.body(), CameraNicknameChangeRequest.class);

            VisionSourceManager.getInstance()
                    .vmm
                    .getModule(request.cameraUniqueName)
                    .setCameraNickname(request.name);
            ctx.status(200);
            ctx.result("Successfully changed the camera name to: " + request.name);
            logger.info("Successfully changed the camera name to: " + request.name);
        } catch (JsonProcessingException e) {
            ctx.status(400).result("Invalid JSON format");
            logger.error("Failed to process camera nickname change request", e);
        } catch (Exception e) {
            ctx.status(500).result("Failed to change camera nickname");
            logger.error("Unexpected error while changing camera nickname", e);
        }
    }

    public static void onMetricsPublishRequest(Context ctx) {
        HardwareManager.getInstance().publishMetrics();
        ctx.status(204);
    }

    /**
     * Get the calibration JSON for a specific observation. Excludes camera image data
     *
     * <p>This is excluded from UICalibrationCoefficients by default to save bandwidth on large
     * calibrations
     */
    public static void onCalibrationJsonRequest(Context ctx) {
        String cameraUniqueName = ctx.queryParam("cameraUniqueName");
        var width = Integer.parseInt(ctx.queryParam("width"));
        var height = Integer.parseInt(ctx.queryParam("height"));

        var module = VisionSourceManager.getInstance().vmm.getModule(cameraUniqueName);
        if (module == null) {
            ctx.status(404);
            return;
        }

        CameraCalibrationCoefficients calList =
                module.getStateAsCameraConfig().calibrations.stream()
                        .filter(
                                it ->
                                        Math.abs(it.unrotatedImageSize.width - width) < 1e-4
                                                && Math.abs(it.unrotatedImageSize.height - height) < 1e-4)
                        .findFirst()
                        .orElse(null);

        if (calList == null) {
            ctx.status(404);
            return;
        }

        ctx.json(calList);
        ctx.status(200);
    }

    private record CalibrationRemoveRequest(int width, int height, String cameraUniqueName) {}

    public static void onCalibrationRemoveRequest(Context ctx) {
        try {
            CalibrationRemoveRequest request =
                    kObjectMapper.readValue(ctx.body(), CalibrationRemoveRequest.class);

            logger.info(
                    "Attempting to remove calibration for camera: "
                            + request.cameraUniqueName
                            + " with a resolution of "
                            + request.width
                            + "x"
                            + request.height);

            VisionSourceManager.getInstance()
                    .vmm
                    .getModule(request.cameraUniqueName)
                    .removeCalibrationFromConfig(new Size(request.width, request.height));

            ctx.status(200);
            ctx.result(
                    "Successfully removed calibration for resolution: "
                            + request.width
                            + "x"
                            + request.height);
            logger.info(
                    "Successfully removed calibration for resolution: "
                            + request.width
                            + "x"
                            + request.height);
        } catch (JsonProcessingException e) {
            ctx.status(400).result("Invalid JSON format");
            logger.error("Failed to process calibration removed request", e);
        } catch (Exception e) {
            ctx.status(500).result("Failed to removed calibration");
            logger.error("Unexpected error while attempting to remove calibration", e);
        }
    }

    public static void onCalibrationSnapshotRequest(Context ctx) {
        String cameraUniqueName = ctx.queryParam("cameraUniqueName");
        var width = Integer.parseInt(ctx.queryParam("width"));
        var height = Integer.parseInt(ctx.queryParam("height"));
        Integer observationIdx = Integer.parseInt(ctx.queryParam("snapshotIdx"));

        CameraCalibrationCoefficients calList =
                VisionSourceManager.getInstance()
                        .vmm
                        .getModule(cameraUniqueName)
                        .getStateAsCameraConfig()
                        .calibrations
                        .stream()
                        .filter(
                                it ->
                                        Math.abs(it.unrotatedImageSize.width - width) < 1e-4
                                                && Math.abs(it.unrotatedImageSize.height - height) < 1e-4)
                        .findFirst()
                        .orElse(null);

        if (calList == null || calList.observations.size() < observationIdx) {
            ctx.status(404);
            return;
        }

        // encode as jpeg to save even more space. reduces size of a 1280p image from 300k to 25k
        var jpegBytes = new MatOfByte();
        var mat = calList.observations.get(observationIdx).annotateImage();
        Imgcodecs.imencode(".jpg", mat, jpegBytes, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 60));
        mat.release();

        ctx.result(jpegBytes.toArray());

        jpegBytes.release();

        ctx.status(200);
    }

    public static void onCalibrationExportRequest(Context ctx) {
        String cameraUniqueName = ctx.queryParam("cameraUniqueName");
        var width = Integer.parseInt(ctx.queryParam("width"));
        var height = Integer.parseInt(ctx.queryParam("height"));

        var cc =
                VisionSourceManager.getInstance().vmm.getModule(cameraUniqueName).getStateAsCameraConfig();

        CameraCalibrationCoefficients calList =
                cc.calibrations.stream()
                        .filter(
                                it ->
                                        Math.abs(it.unrotatedImageSize.width - width) < 1e-4
                                                && Math.abs(it.unrotatedImageSize.height - height) < 1e-4)
                        .findFirst()
                        .orElse(null);

        if (calList == null) {
            ctx.status(404);
            return;
        }

        var filename = "photon_calibration_" + cc.uniqueName + "_" + width + "x" + height + ".json";
        ctx.contentType("application/zip");
        ctx.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        ctx.json(calList);

        ctx.status(200);
    }

    public static void onImageSnapshotsRequest(Context ctx) {
        var snapshots = new ArrayList<HashMap<String, Object>>();
        var cameraDirs = ConfigManager.getInstance().getImageSavePath().toFile().listFiles();

        if (cameraDirs != null) {
            try {
                for (File cameraDir : cameraDirs) {
                    var cameraSnapshots = cameraDir.listFiles();
                    if (cameraSnapshots == null) continue;

                    String cameraUniqueName = cameraDir.getName();

                    for (File snapshot : cameraSnapshots) {
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

    public static void onCameraCalibImagesRequest(Context ctx) {
        try {
            HashMap<String, HashMap<String, ArrayList<HashMap<String, Object>>>> snapshots =
                    new HashMap<>();

            var cameraDirs = ConfigManager.getInstance().getCalibDir().toFile().listFiles();
            if (cameraDirs != null) {
                var camData = new HashMap<String, ArrayList<HashMap<String, Object>>>();
                for (var cameraDir : cameraDirs) {
                    var resolutionDirs = cameraDir.listFiles();
                    if (resolutionDirs == null) continue;
                    for (var resolutionDir : resolutionDirs) {
                        var calibImages = resolutionDir.listFiles();
                        if (calibImages == null) continue;
                        var resolutionImages = new ArrayList<HashMap<String, Object>>();
                        for (var calibImg : calibImages) {
                            var snapshotData = new HashMap<String, Object>();

                            var bufferedImage = ImageIO.read(calibImg);
                            var buffer = new ByteArrayOutputStream();
                            ImageIO.write(bufferedImage, "png", buffer);
                            byte[] data = buffer.toByteArray();

                            snapshotData.put("snapshotData", data);
                            snapshotData.put("snapshotFilename", calibImg.getName());

                            resolutionImages.add(snapshotData);
                        }
                        camData.put(resolutionDir.getName(), resolutionImages);
                    }

                    var cameraName = cameraDir.getName();
                    snapshots.put(cameraName, camData);
                }
            }

            ctx.json(snapshots);
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("An error occurred while getting calib data");
            logger.error("An error occurred while getting calib data", e);
        }
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

    public static void onNukeConfigDirectory(Context ctx) {
        ConfigManager.getInstance().setWriteTaskEnabled(false);
        ConfigManager.getInstance().disableFlushOnShutdown();

        Logger.closeAllLoggers();
        if (ConfigManager.nukeConfigDirectory()) {
            ctx.status(200);
            ctx.result("Successfully nuked config dir");
            restartProgram();
        } else {
            ctx.status(500);
            ctx.result("There was an error while nuking the config directory");
        }
    }

    public static void onNukeOneCamera(Context ctx) {
        try {
            CommonCameraUniqueName request =
                    kObjectMapper.readValue(ctx.body(), CommonCameraUniqueName.class);

            logger.warn("Deleting camera name " + request.cameraUniqueName);

            var cameraDir =
                    ConfigManager.getInstance()
                            .getCalibrationImageSavePath(request.cameraUniqueName)
                            .toFile();
            if (cameraDir.exists()) {
                FileUtils.deleteDirectory(cameraDir);
            }

            VisionSourceManager.getInstance().deleteVisionSource(request.cameraUniqueName);

            ctx.status(200);
        } catch (IOException e) {
            logger.error("Failed to delete camera", e);
            ctx.status(500);
            ctx.result("Failed to delete camera");
            return;
        }
    }

    public static void onActivateMatchedCameraRequest(Context ctx) {
        logger.info(ctx.queryString());
        try {
            CommonCameraUniqueName request =
                    kObjectMapper.readValue(ctx.body(), CommonCameraUniqueName.class);

            if (VisionSourceManager.getInstance()
                    .reactivateDisabledCameraConfig(request.cameraUniqueName)) {
                ctx.status(200);
            } else {
                ctx.status(403);
            }
        } catch (IOException e) {
            ctx.status(401);
            logger.error("Failed to process activate matched camera request", e);
            ctx.result("Failed to process activate matched camera request");
            return;
        }
    }

    private record AssignUnmatchedCamera(PVCameraInfo cameraInfo) {}

    public static void onAssignUnmatchedCameraRequest(Context ctx) {
        logger.info(ctx.queryString());

        try {
            AssignUnmatchedCamera request =
                    kObjectMapper.readValue(ctx.body(), AssignUnmatchedCamera.class);

            if (request.cameraInfo == null) {
                ctx.status(400);
                ctx.result("cameraInfo is required");
                logger.error("cameraInfo is missing in the request");
                return;
            }

            if (VisionSourceManager.getInstance().assignUnmatchedCamera(request.cameraInfo)) {
                ctx.status(200);
            } else {
                ctx.status(404);
            }

            ctx.result("Successfully assigned camera: " + request.cameraInfo);
        } catch (IOException e) {
            ctx.status(401);
            logger.error("Failed to process assign unmatched camera request", e);
            ctx.result("Failed to process assign unmatched camera request");
            return;
        }
    }

    public static void onUnassignCameraRequest(Context ctx) {
        logger.info(ctx.queryString());
        try {
            CommonCameraUniqueName request =
                    kObjectMapper.readValue(ctx.body(), CommonCameraUniqueName.class);

            if (VisionSourceManager.getInstance().deactivateVisionSource(request.cameraUniqueName)) {
                ctx.status(200);
            } else {
                ctx.status(403);
            }
        } catch (IOException e) {
            ctx.status(401);
            logger.error("Failed to process unassign camera request", e);
            ctx.result("Failed to process unassign camera request");
            return;
        }
    }
}

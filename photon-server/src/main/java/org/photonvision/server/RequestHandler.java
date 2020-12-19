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
import edu.wpi.first.math.geometry.Rotation2d;
import io.javalin.http.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.MetricsPublisher;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.ShellExec;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.target.TargetModel;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

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
            restartProgram(ctx);

        } else {
            logger.error("Couldn't read uploaded file! Ignoring.");
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

            // The only settings we actually care about are FOV and pitch
            var fov = Double.parseDouble(settings.get("fov").toString());
            var pitch =
                    Rotation2d.fromDegrees(Double.parseDouble(settings.get("tiltDegrees").toString()));

            logger.info(
                    String.format(
                            "Setting camera %s's fov to %s w/pitch %s", index, fov, pitch.getDegrees()));
            var module = VisionModuleManager.getInstance().getModule(index);
            module.setFovAndPitch(fov, pitch);
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

    /**
    * Note that this doesn't actually restart the program itself -- instead, it relies on systemd or
    * an equivalent.
    */
    public static void restartProgram(Context ctx) {
        ctx.status(200);

        if (Platform.isRaspberryPi()) {
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
        MetricsPublisher.getInstance().publish();
        ctx.status(200);
    }

    public static class UITargetData {
        public int index;
        public TargetModel targetModel;
    }

    public static void getSnapshotList(Context ctx) {
        var path = ctx.queryParam("cam");
        if(path == null) return;
        logger.debug("getting snapshots for cam " + path);
        var paths = ConfigManager.getInstance().getSnapshots(path);
        ctx.json(paths);
        ctx.status(200);
    }

    public static void getSnapshot(Context ctx) {
        var path = ctx.queryParam("path");
        if(path == null) return;
        logger.debug("getting path at " + path);
        try {
            var snapshot = new FileInputStream(ConfigManager.getInstance().getSnapshotFile(path));
            ctx.result(snapshot);
            ctx.contentType("image/jpeg");
            ctx.status(200);
        } catch (Exception e) {
            logger.error("Exception finding uploaded snapshot at " + path, e);
            ctx.status(501);
        }
    }
}

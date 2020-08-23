/*
 * Copyright (C) 2020 Photon Vision.
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import io.javalin.http.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Point3;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.target.TargetModel;

public class RequestHandler {
    private static final Logger logger = new Logger(RequestHandler.class, LogGroup.WebServer);

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    public static void onSettingUpload(Context ctx) {
        var file = ctx.uploadedFile("zipData");
        if (file != null) {
            var tempZipPath =
                new File(Path.of(System.getProperty("java.io.tmpdir"), file.getFilename()).toString());
            tempZipPath.getParentFile().mkdirs();
            try {
                FileUtils.copyInputStreamToFile(file.getContent(), tempZipPath);
            } catch (IOException e) {
                logger.error("Exception uploading settings file!");
                e.printStackTrace();
            }
            ConfigManager.saveUploadedSettingsZip(tempZipPath);
            //            restartDevice();
        } else {
            logger.error("Couldn't read uploaded settings ZIP! Ignoring.");
        }
    }

    @SuppressWarnings("unchecked")
    public static void onGeneralSettings(Context context) throws JsonProcessingException {
        Map<String, Object> map =
            (Map<String, Object>) kObjectMapper.readValue(context.body(), Map.class);
        var networking =
            (Map<String, Object>)
                map.get("networkSettings"); // teamNumber (int), supported (bool), connectionType (int),
        // staticIp (str), netmask (str), gateway (str), hostname (str)
        var lighting =
            (Map<String, Object>) map.get("lighting"); // supported (true/false), brightness (int)
        // TODO do stuff with lighting

        var networkConfig = NetworkConfig.fromHashMap(networking);
        ConfigManager.getInstance().setNetworkSettings(networkConfig);
        ConfigManager.getInstance().requestSave();
        NetworkManager.getInstance().reinitialize();
        NetworkTablesManager.getInstance().setConfig(networkConfig);

        logger.info("Responding to general settings with http 200");
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
        var index = Integer.parseInt(ctx.body());
        var calData = VisionModuleManager.getInstance().getModule(index).endCalibration();
        if (calData == null) {
            ctx.status(500);
            return;
        }

        ctx.result(String.valueOf(calData.standardDeviation));
        ctx.status(200);
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
        System.exit(0);
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

    public static class UITargetData {
        public int index;
        public TargetModel targetModel;
    }
}

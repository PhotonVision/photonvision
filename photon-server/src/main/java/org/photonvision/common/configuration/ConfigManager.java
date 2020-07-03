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

package org.photonvision.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;

public class ConfigManager {
    private static final Logger logger = new Logger(ConfigManager.class, LogGroup.General);
    private static ConfigManager INSTANCE;

    private PhotonConfiguration config;
    private final File rootFolder;
    private final File hardwareConfigFile;
    private final File networkConfigFile;
    private final File camerasFolder;

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager(getRootFolder());
        }
        return INSTANCE;
    }

    public PhotonConfiguration getConfig() {
        return config;
    }

    protected static Path getRootFolder() {
        return Path.of("photonvision");
    }

    private ConfigManager(Path rootFolder) {
        this.rootFolder = new File(rootFolder.toUri());
        this.hardwareConfigFile =
                new File(Path.of(rootFolder.toString(), "hardwareConfig.json").toUri());
        this.networkConfigFile =
                new File(Path.of(rootFolder.toString(), "networkSettings.json").toUri());
        this.camerasFolder = new File(Path.of(rootFolder.toString(), "cameras").toUri());
        load();
    }

    public void load() {
        logger.info("Loading settings...");
        if (!rootFolder.exists()) {
            if (rootFolder.mkdirs()) {
                logger.debug("Root config folder did not exist. Created!");
            } else {
                logger.error("Failed to create root config folder!");
            }
        }

        HardwareConfig hardwareConfig;
        NetworkConfig networkConfig;

        if (hardwareConfigFile.exists()) {
            try {
                hardwareConfig =
                        JacksonUtils.deserialize(hardwareConfigFile.toPath(), HardwareConfig.class);
                if (hardwareConfig == null) {
                    logger.error("Could not deserialize hardware config! Loading defaults");
                    hardwareConfig = new HardwareConfig();
                }
            } catch (IOException e) {
                logger.error("Could not deserialize hardware config! Loading defaults");
                hardwareConfig = new HardwareConfig();
            }
        } else {
            logger.info("Hardware config does not exist! Loading defaults");
            hardwareConfig = new HardwareConfig();
        }

        if (networkConfigFile.exists()) {
            try {
                networkConfig = JacksonUtils.deserialize(networkConfigFile.toPath(), NetworkConfig.class);
                if (networkConfig == null) {
                    logger.error("Could not deserialize network config! Loading defaults");
                    networkConfig = new NetworkConfig();
                }
            } catch (IOException e) {
                logger.error("Could not deserialize network config! Loading defaults");
                networkConfig = new NetworkConfig();
            }
        } else {
            logger.info("Network config file does not exist! Loading defaults");
            networkConfig = new NetworkConfig();
        }

        if (!camerasFolder.exists()) {
            if (camerasFolder.mkdirs()) {
                logger.debug("Cameras config folder did not exist. Created!");
            } else {
                logger.error("Failed to create cameras config folder!");
            }
        }

        HashMap<String, CameraConfiguration> cameraConfigurations = loadCameraConfigs();

        this.config = new PhotonConfiguration(hardwareConfig, networkConfig, cameraConfigurations);
    }

    public void save() {
        logger.info("Saving settings...");

        try {
            JacksonUtils.serializer(hardwareConfigFile.toPath(), config.getHardwareConfig());
        } catch (IOException e) {
            logger.error("Could not save hardware config!");
            e.printStackTrace();
        }
        try {
            JacksonUtils.serializer(networkConfigFile.toPath(), config.getNetworkConfig());
        } catch (IOException e) {
            logger.error("Could not save network config!");
            e.printStackTrace();
        }

        // save all of our cameras
        var cameraConfigMap = config.getCameraConfigurations();
        for (var subdirName : cameraConfigMap.keySet()) {
            var camConfig = cameraConfigMap.get(subdirName);
            var subdir = Path.of(camerasFolder.toPath().toString(), subdirName);

            if (!subdir.toFile().exists()) {
                subdir.toFile().mkdirs();
            }

            try {
                JacksonUtils.serializer(Path.of(subdir.toString(), "config.json"), camConfig);
            } catch (IOException e) {
                logger.error("Could not save config.json for " + subdir);
            }

            try {
                JacksonUtils.serializer(
                        Path.of(subdir.toString(), "drivermode.json"), camConfig.driveModeSettings);
            } catch (IOException e) {
                logger.error("Could not save drivermode.json for " + subdir);
            }

            for (var pipe : camConfig.pipelineSettings) {
                var pipePath = Path.of(subdir.toString(), "pipelines", pipe.pipelineNickname + ".json");

                if (!pipePath.getParent().toFile().exists()) {
                    pipePath.getParent().toFile().mkdirs();
                }

                try {
                    JacksonUtils.serializer(pipePath, pipe);
                } catch (IOException e) {
                    logger.error("Could not save " + pipe.pipelineNickname + ".json!");
                }
            }
        }
    }

    private HashMap<String, CameraConfiguration> loadCameraConfigs() {
        HashMap<String, CameraConfiguration> loadedConfigurations = new HashMap<>();
        try {
            var subdirectories =
                    Files.list(camerasFolder.toPath())
                            .filter(f -> f.toFile().isDirectory())
                            .collect(Collectors.toList());

            for (var subdir : subdirectories) {
                var cameraConfigPath = Path.of(subdir.toString(), "config.json");
                CameraConfiguration loadedConfig = null;
                try {
                    loadedConfig =
                            JacksonUtils.deserialize(
                                    cameraConfigPath.toAbsolutePath(), CameraConfiguration.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (loadedConfig == null) { // If the file could not be deserialized
                    logger.warn("Could not load camera " + subdir + "'s config.json! Loading " + "default");
                    continue; // TODO how do we later try to load this camera if it gets reconnected?
                }

                // At this point we have only loaded the base stuff
                // We still need to deserialize pipelines, as well as
                // driver mode settings
                var driverModeFile = Path.of(subdir.toString(), "drivermode.json");
                DriverModePipelineSettings driverMode;
                try {
                    driverMode =
                            JacksonUtils.deserialize(
                                    driverModeFile.toAbsolutePath(), DriverModePipelineSettings.class);
                } catch (JsonProcessingException e) {
                    logger.error("Could not deserialize drivermode.json! Loading defaults");
                    logger.de_pest(Arrays.toString(e.getStackTrace()));
                    driverMode = new DriverModePipelineSettings();
                }
                if (driverMode == null) {
                    logger.warn(
                            "Could not load camera " + subdir + "'s drivermode.json! Loading" + " default");
                    driverMode = new DriverModePipelineSettings();
                }

                // Load pipelines by mapping the files within the pipelines subdir
                // to their deserialized equivalents
                List<CVPipelineSettings> settings =
                        Files.list(Path.of(subdir.toString(), "pipelines"))
                                .filter(p -> p.toFile().isFile())
                                .map(
                                        p -> {
                                            var relativizedFilePath =
                                                    getRootFolder().toAbsolutePath().relativize(p).toString();
                                            try {
                                                var ret = JacksonUtils.deserialize(p, CVPipelineSettings.class);
                                                return ret;

                                            } catch (JsonProcessingException e) {
                                                logger.error("Exception while deserializing " + relativizedFilePath);
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                logger.warn(
                                                        "Could not load pipeline at " + relativizedFilePath + "! Skipping...");
                                            }
                                            return null;
                                        })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                loadedConfig.driveModeSettings = driverMode;
                loadedConfig.addPipelineSettings(settings);

                loadedConfigurations.put(subdir.toFile().getName(), loadedConfig);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedConfigurations;
    }
}

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;
import org.photonvision.vision.processes.VisionSource;
import org.zeroturnaround.zip.ZipUtil;

public class ConfigManager {
    private static final Logger logger = new Logger(ConfigManager.class, LogGroup.General);
    private static ConfigManager INSTANCE;

    private PhotonConfiguration config;
    private final File hardwareConfigFile;
    private final File networkConfigFile;
    private final File camerasFolder;

    final File configDirectoryFile;

    private long saveRequestTimestamp = -1;

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager(getRootFolder());
        }
        return INSTANCE;
    }

    public static void saveUploadedSettingsZip(File uploadPath) {
        logger.info(uploadPath.getAbsolutePath());
        var folderPath = Path.of(System.getProperty("java.io.tmpdir"), "photonvision").toFile();
        folderPath.mkdirs();
        ZipUtil.unpack(uploadPath, folderPath);
        FileUtils.deleteDirectory(getRootFolder());
        try {
            org.apache.commons.io.FileUtils.copyDirectory(folderPath, getRootFolder().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(666);
    }

    public PhotonConfiguration getConfig() {
        return config;
    }

    private static Path getRootFolder() {
        return Path.of("photonvision_config");
    }

    ConfigManager(Path configDirectoryFile) {
        this.configDirectoryFile = new File(configDirectoryFile.toUri());
        this.hardwareConfigFile =
                new File(Path.of(configDirectoryFile.toString(), "hardwareConfig.json").toUri());
        this.networkConfigFile =
                new File(Path.of(configDirectoryFile.toString(), "networkSettings.json").toUri());
        this.camerasFolder = new File(Path.of(configDirectoryFile.toString(), "cameras").toUri());

        TimedTaskManager.getInstance().addTask("ConfigManager", this::checkSaveAndWrite, 1000);
    }

    public void load() {
        logger.info("Loading settings...");
        if (!configDirectoryFile.exists()) {
            if (configDirectoryFile.mkdirs()) {
                logger.debug("Root config folder did not exist. Created!");
            } else {
                logger.error("Failed to create root config folder!");
            }
        }
        if (!configDirectoryFile.canWrite()) {
            logger.debug("Making root dir writeable...");
            try {
                var success = configDirectoryFile.setWritable(true);
                if (success) logger.debug("Set root dir writeable!");
                else logger.error("Could not make root dir writeable!");
            } catch (SecurityException e) {
                logger.error("Could not make root dir writeable!", e);
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

    public void saveToDisk() {
        logger.info("Saving settings...");

        // Delete old configs
        FileUtils.deleteDirectory(camerasFolder.toPath());

        try {
            JacksonUtils.serialize(hardwareConfigFile.toPath(), config.getHardwareConfig());
        } catch (IOException e) {
            logger.error("Could not save hardware config!", e);
        }
        try {
            JacksonUtils.serialize(networkConfigFile.toPath(), config.getNetworkConfig());
        } catch (IOException e) {
            logger.error("Could not save network config!", e);
        }

        // save all of our cameras
        var cameraConfigMap = config.getCameraConfigurations();
        for (var subdirName : cameraConfigMap.keySet()) {
            var camConfig = cameraConfigMap.get(subdirName);
            var subdir = Path.of(camerasFolder.toPath().toString(), subdirName);

            if (!subdir.toFile().exists()) {
                // TODO: check for error
                subdir.toFile().mkdirs();
            }

            try {
                JacksonUtils.serialize(Path.of(subdir.toString(), "config.json"), camConfig);
            } catch (IOException e) {
                logger.error("Could not save config.json for " + subdir, e);
            }

            try {
                JacksonUtils.serialize(
                        Path.of(subdir.toString(), "drivermode.json"), camConfig.driveModeSettings);
            } catch (IOException e) {
                logger.error("Could not save drivermode.json for " + subdir, e);
            }

            for (var pipe : camConfig.pipelineSettings) {
                var pipePath = Path.of(subdir.toString(), "pipelines", pipe.pipelineNickname + ".json");

                if (!pipePath.getParent().toFile().exists()) {
                    // TODO: check for error
                    pipePath.getParent().toFile().mkdirs();
                }

                try {
                    JacksonUtils.serialize(pipePath, pipe);
                } catch (IOException e) {
                    logger.error("Could not save " + pipe.pipelineNickname + ".json!", e);
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
                    logger.error("Camera config deserialization failed!", e);
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
                    logger.debug(Arrays.toString(e.getStackTrace()));
                    driverMode = new DriverModePipelineSettings();
                }
                if (driverMode == null) {
                    logger.warn(
                            "Could not load camera " + subdir + "'s drivermode.json! Loading" + " default");
                    driverMode = new DriverModePipelineSettings();
                }

                // Load pipelines by mapping the files within the pipelines subdir
                // to their deserialized equivalents
                var pipelineSubdirectory = Path.of(subdir.toString(), "pipelines");
                List<CVPipelineSettings> settings =
                        pipelineSubdirectory.toFile().exists()
                                ? Files.list(pipelineSubdirectory)
                                        .filter(p -> p.toFile().isFile())
                                        .map(
                                                p -> {
                                                    var relativizedFilePath =
                                                            configDirectoryFile
                                                                    .toPath()
                                                                    .toAbsolutePath()
                                                                    .relativize(p)
                                                                    .toString();
                                                    try {
                                                        return JacksonUtils.deserialize(p, CVPipelineSettings.class);
                                                    } catch (JsonProcessingException e) {
                                                        logger.error("Exception while deserializing " + relativizedFilePath, e);
                                                    } catch (IOException e) {
                                                        logger.warn(
                                                                "Could not load pipeline at "
                                                                        + relativizedFilePath
                                                                        + "! Skipping...");
                                                    }
                                                    return null;
                                                })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                                : Collections.emptyList();

                loadedConfig.driveModeSettings = driverMode;
                loadedConfig.addPipelineSettings(settings);

                loadedConfigurations.put(subdir.toFile().getName(), loadedConfig);
            }
        } catch (IOException e) {
            logger.error("Error loading camera configs!", e);
        }
        return loadedConfigurations;
    }

    public void addCameraConfigurations(HashMap<VisionSource, List<CVPipelineSettings>> sources) {
        List<CameraConfiguration> list =
                sources.keySet().stream()
                        .map(it -> it.getSettables().getConfiguration())
                        .collect(Collectors.toList());
        getConfig().addCameraConfigs(list);
        requestSave();
    }

    public void saveModule(CameraConfiguration config, String uniqueName) {
        getConfig().addCameraConfig(uniqueName, config);
        requestSave();
    }

    public File getSettingsFolderAsZip() {
        File out = Path.of(System.getProperty("java.io.tmpdir"), "photonvision-settings.zip").toFile();
        try {
            ZipUtil.pack(configDirectoryFile, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public void setNetworkSettings(NetworkConfig networkConfig) {
        getConfig().setNetworkConfig(networkConfig);
        requestSave();
    }

    public Path getLogPath() {
        var dateString = DateTimeFormatter.ofPattern("yyyy-M-d_hh-mm-ss").format(LocalDateTime.now());
        var logFile =
                Path.of(configDirectoryFile.toString(), "logs", "photonvision-" + dateString + ".log")
                        .toFile();
        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
        return logFile.toPath();
    }

    public void requestSave() {
        logger.debug("Requesting save...");
        saveRequestTimestamp = System.currentTimeMillis();
    }

    private void checkSaveAndWrite() {
        // Only save if 1 second has past since the request was made
        if (saveRequestTimestamp > 0 && (System.currentTimeMillis() - saveRequestTimestamp) > 1000L) {
            saveRequestTimestamp = -1;
            logger.debug("Saving to disk...");
            saveToDisk();
        }
    }
}

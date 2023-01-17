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

package org.photonvision.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;
import org.photonvision.vision.processes.VisionSource;
import org.zeroturnaround.zip.ZipUtil;

public class ConfigManager {
    private static final Logger logger = new Logger(ConfigManager.class, LogGroup.General);
    private static ConfigManager INSTANCE;

    public static final String HW_CFG_FNAME = "hardwareConfig.json";
    public static final String HW_SET_FNAME = "hardwareSettings.json";
    public static final String NET_SET_FNAME = "networkSettings.json";

    private PhotonConfiguration config;
    private final File hardwareConfigFile;
    private final File hardwareSettingsFile;
    private final File networkConfigFile;
    private final File camerasFolder;

    final File configDirectoryFile;

    private long saveRequestTimestamp = -1;
    private Thread settingsSaveThread;

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager(getRootFolder());
        }
        return INSTANCE;
    }

    public static void saveUploadedSettingsZip(File uploadPath) {
        var folderPath = Path.of(System.getProperty("java.io.tmpdir"), "photonvision").toFile();
        folderPath.mkdirs();
        ZipUtil.unpack(uploadPath, folderPath);
        FileUtils.deleteDirectory(getRootFolder());
        try {
            org.apache.commons.io.FileUtils.copyDirectory(folderPath, getRootFolder().toFile());
            logger.info("Copied settings successfully!");
        } catch (IOException e) {
            logger.error("Exception copying uploaded settings!", e);
            return;
        }
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
                new File(Path.of(configDirectoryFile.toString(), HW_CFG_FNAME).toUri());
        this.hardwareSettingsFile =
                new File(Path.of(configDirectoryFile.toString(), HW_SET_FNAME).toUri());
        this.networkConfigFile =
                new File(Path.of(configDirectoryFile.toString(), NET_SET_FNAME).toUri());
        this.camerasFolder = new File(Path.of(configDirectoryFile.toString(), "cameras").toUri());

        settingsSaveThread = new Thread(this::saveAndWriteTask);
        settingsSaveThread.start();
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
        HardwareSettings hardwareSettings;
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

        if (hardwareSettingsFile.exists()) {
            try {
                hardwareSettings =
                        JacksonUtils.deserialize(hardwareSettingsFile.toPath(), HardwareSettings.class);
                if (hardwareSettings == null) {
                    logger.error("Could not deserialize hardware settings! Loading defaults");
                    hardwareSettings = new HardwareSettings();
                }
            } catch (IOException e) {
                logger.error("Could not deserialize hardware settings! Loading defaults");
                hardwareSettings = new HardwareSettings();
            }
        } else {
            logger.info("Hardware settings does not exist! Loading defaults");
            hardwareSettings = new HardwareSettings();
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

        this.config =
                new PhotonConfiguration(
                        hardwareConfig, hardwareSettings, networkConfig, cameraConfigurations);
    }

    public void saveToDisk() {
        // Delete old configs
        FileUtils.deleteDirectory(camerasFolder.toPath());

        try {
            JacksonUtils.serialize(networkConfigFile.toPath(), config.getNetworkConfig());
        } catch (IOException e) {
            logger.error("Could not save network config!", e);
        }
        try {
            JacksonUtils.serialize(hardwareSettingsFile.toPath(), config.getHardwareSettings());
        } catch (IOException e) {
            logger.error("Could not save hardware config!", e);
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
        logger.info("Settings saved!");
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

    public void addCameraConfigurations(List<VisionSource> sources) {
        getConfig().addCameraConfigs(sources);
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

    public Path getLogsDir() {
        return Path.of(configDirectoryFile.toString(), "logs");
    }

    public Path getCalibDir() {
        return Path.of(configDirectoryFile.toString(), "calibImgs");
    }

    public static final String LOG_PREFIX = "photonvision-";
    public static final String LOG_EXT = ".log";
    public static final String LOG_DATE_TIME_FORMAT = "yyyy-M-d_hh-mm-ss";

    public String taToLogFname(TemporalAccessor date) {
        var dateString = DateTimeFormatter.ofPattern(LOG_DATE_TIME_FORMAT).format(date);
        return LOG_PREFIX + dateString + LOG_EXT;
    }

    public Date logFnameToDate(String fname) throws ParseException {
        // Strip away known unneded portions of the log file name
        fname = fname.replace(LOG_PREFIX, "").replace(LOG_EXT, "");
        DateFormat format = new SimpleDateFormat(LOG_DATE_TIME_FORMAT);
        return format.parse(fname);
    }

    public Path getLogPath() {
        var logFile = Path.of(this.getLogsDir().toString(), taToLogFname(LocalDateTime.now())).toFile();
        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
        return logFile.toPath();
    }

    public Path getImageSavePath() {
        var imgFilePath = Path.of(configDirectoryFile.toString(), "imgSaves").toFile();
        if (!imgFilePath.exists()) imgFilePath.mkdirs();
        return imgFilePath.toPath();
    }

    public Path getHardwareConfigFile() {
        return this.hardwareConfigFile.toPath();
    }

    public Path getHardwareSettingsFile() {
        return this.hardwareSettingsFile.toPath();
    }

    public Path getNetworkConfigFile() {
        return this.networkConfigFile.toPath();
    }

    public void saveUploadedHardwareConfig(Path uploadPath) {
        FileUtils.deleteFile(this.getHardwareConfigFile());
        FileUtils.copyFile(uploadPath, this.getHardwareConfigFile());
    }

    public void saveUploadedHardwareSettings(Path uploadPath) {
        FileUtils.deleteFile(this.getHardwareSettingsFile());
        FileUtils.copyFile(uploadPath, this.getHardwareSettingsFile());
    }

    public void saveUploadedNetworkConfig(Path uploadPath) {
        FileUtils.deleteFile(this.getNetworkConfigFile());
        FileUtils.copyFile(uploadPath, this.getNetworkConfigFile());
    }

    public void requestSave() {
        logger.trace("Requesting save...");
        saveRequestTimestamp = System.currentTimeMillis();
    }

    private void saveAndWriteTask() {
        // Only save if 1 second has past since the request was made
        while (!Thread.currentThread().isInterrupted()) {
            if (saveRequestTimestamp > 0 && (System.currentTimeMillis() - saveRequestTimestamp) > 1000L) {
                saveRequestTimestamp = -1;
                logger.debug("Saving to disk...");
                saveToDisk();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Exception waiting for settings semaphore", e);
            }
        }
    }

    public void unloadCameraConfigs() {
        this.config.getCameraConfigurations().clear();
    }
}

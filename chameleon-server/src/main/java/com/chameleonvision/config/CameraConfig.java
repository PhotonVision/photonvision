package com.chameleonvision.config;

import com.chameleonvision.util.FileHelper;
import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CameraConfig {

    private static final Path camerasConfigFolderPath = Paths.get(ConfigManager.SettingsPath.toString(), "cameras");

    private final String cameraConfigName;
    private final CameraJsonConfig preliminaryConfig;

    private final Path configFolderPath;
    private final Path configPath;
    private final Path driverModePath;
    final Path pipelineFolderPath;

    public final PipelineConfig pipelineConfig;

    CameraConfig(CameraJsonConfig config) {
        preliminaryConfig = config;
        cameraConfigName = preliminaryConfig.name.replace(' ', '_');
        pipelineConfig = new PipelineConfig(this);

        configFolderPath = Paths.get(camerasConfigFolderPath.toString(), cameraConfigName);
        configPath = Paths.get(configFolderPath.toString(), "camera.json");
        driverModePath = Paths.get(configFolderPath.toString(), "drivermode.json");
        pipelineFolderPath = Paths.get(configFolderPath.toString(), "pipelines");
    }

    public FullCameraConfiguration load() {
        checkFolder();
        checkConfig();
        checkDriverMode();
        pipelineConfig.check();

        return new FullCameraConfiguration(loadConfig(), pipelineConfig.load(), loadDriverMode(), this);
    }

    private CameraJsonConfig loadConfig() {
        CameraJsonConfig config = preliminaryConfig;
        try {
            config = JacksonHelper.deserializer(configPath, CameraJsonConfig.class);
        } catch (IOException e) {
            System.err.printf("Failed to load camera config: %s - using default.\n", configPath.toString());
        }
        return config;
    }

    private CVPipelineSettings loadDriverMode() {
        CVPipelineSettings driverMode = new CVPipelineSettings();
        driverMode.nickname = "DRIVERMODE";
        try {
            driverMode = JacksonHelper.deserializer(driverModePath, CVPipelineSettings.class);
        } catch (IOException e) {
            System.err.println("Failed to load camera drivermode: " + driverModePath.toString());
        }
        return driverMode;
    }

    void saveConfig(CameraJsonConfig config) {
        try {
            JacksonHelper.serializer(configPath, config);
            FileHelper.setFilePerms(configPath);
        } catch (IOException e) {
            System.err.println("Failed to save camera config file: " + configPath.toString());
        }
    }

    void savePipelines(List<CVPipelineSettings> pipelines) {
        pipelineConfig.save(pipelines);
    }

    public void saveDriverMode(CVPipelineSettings driverMode) {
        try {
            JacksonHelper.serializer(driverModePath, driverMode);
            FileHelper.setFilePerms(driverModePath);
        } catch (IOException e) {
            System.err.println("Failed to save camera drivermode file: " + driverModePath.toString());
        }
    }

    void checkFolder() {
        if (!getConfigFolderExists()) {
            try {
                if (!(new File(configFolderPath.toUri()).mkdirs())) {
                    System.err.println("Failed to create camera config folder: " + configFolderPath.toString());
                }
                FileHelper.setFilePerms(configFolderPath);
            } catch(Exception e) {
                System.err.println("Failed to create camera config folder: " + configFolderPath.toString());
            }
        }
    }

    private void checkConfig() {
        if (!configExists()) {
            try {
                JacksonHelper.serializer(configPath, preliminaryConfig);
                FileHelper.setFilePerms(configPath);
            } catch (IOException e) {
                System.err.println("Failed to create camera config file: " + configPath.toString());
            }
        }
    }

    private void checkDriverMode() {
        if (!driverModeExists()) {
            try {
                CVPipelineSettings newDriverModeSettings = new CVPipelineSettings();
                newDriverModeSettings.nickname = "DRIVERMODE";
                JacksonHelper.serializer(driverModePath, newDriverModeSettings);
                FileHelper.setFilePerms(driverModePath);
            } catch (IOException e) {
                System.err.println("Failed to create camera drivermode file: " + driverModePath.toString());
            }
        }
    }

    private boolean getConfigFolderExists() {
        return Files.exists(configFolderPath);
    }

    private boolean configExists() {
        return getConfigFolderExists() && Files.exists(configPath);
    }

    private boolean driverModeExists() {
        return getConfigFolderExists() && Files.exists(driverModePath);
    }
}

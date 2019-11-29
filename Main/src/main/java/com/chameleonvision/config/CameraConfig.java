package com.chameleonvision.config;

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

    public final PipelineConfig pipelineConfig;

    CameraConfig(CameraJsonConfig config) {
        preliminaryConfig = config;
        cameraConfigName = preliminaryConfig.name.replace(' ', '_');
        pipelineConfig = new PipelineConfig(this);
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
            config = JacksonHelper.deserializer(getConfigPath(), CameraJsonConfig.class);
        } catch (IOException e) {
            System.err.printf("Failed to load camera config: %s - using default.\n", getConfigPath().toString());
        }
        return config;
    }

    private CVPipelineSettings loadDriverMode() {
        CVPipelineSettings driverMode = new CVPipelineSettings();
        driverMode.nickname = "DRIVERMODE";
        try {
            driverMode = JacksonHelper.deserializer(getDriverModePath(), CVPipelineSettings.class);
        } catch (IOException e) {
            System.err.println("Failed to load camera drivermode: " + getDriverModePath().toString());
        }
        return driverMode;
    }

    void saveConfig(CameraJsonConfig config) {
        try {
            JacksonHelper.serializer(getConfigPath(), config);
        } catch (IOException e) {
            System.err.println("Failed to save camera config file: " + getConfigPath().toString());
        }
    }

    void savePipelines(List<CVPipelineSettings> pipelines) {
        pipelineConfig.save(pipelines);
    }

    void saveDriverMode(CVPipelineSettings driverMode) {
        try {
            JacksonHelper.serializer(getDriverModePath(), driverMode);
        } catch (IOException e) {
            System.err.println("Failed to save camera drivermode file: " + getDriverModePath().toString());
        }
    }

    void checkFolder() {
        if (!getConfigFolderExists()) {
            try {
                if (!(new File(getConfigFolderPath().toUri()).mkdirs())) {
                    System.err.println("Failed to create camera config folder: " + getConfigFolderPath().toString());
                }
            } catch(Exception e) {
                System.err.println("Failed to create camera config folder: " + getConfigFolderPath().toString());
            }
        }
    }

    private void checkConfig() {
        if (!configExists()) {
            try {
                JacksonHelper.serializer(getConfigPath(), preliminaryConfig);
            } catch (IOException e) {
                System.err.println("Failed to create camera config file: " + getConfigPath().toString());
            }
        }
    }

    private void checkDriverMode() {
        if (!driverModeExists()) {
            try {
                CVPipelineSettings newDriverModeSettings = new CVPipelineSettings();
                newDriverModeSettings.nickname = "DRIVERMODE";
                JacksonHelper.serializer(getDriverModePath(), newDriverModeSettings);
            } catch (IOException e) {
                System.err.println("Failed to create camera drivermode file: " + getDriverModePath().toString());
            }
        }
    }

    private Path getConfigFolderPath() {
        return Paths.get(camerasConfigFolderPath.toString(), cameraConfigName);
    }

    private Path getConfigPath() {
        return Paths.get(getConfigFolderPath().toString(), "camera.json");
    }

    private Path getDriverModePath() {
        return Paths.get(getConfigFolderPath().toString(), "drivermode.json");
    }

    private boolean getConfigFolderExists() {
        return Files.exists(getConfigFolderPath());
    }

    Path getPipelineFolderPath() {
        return Paths.get(getConfigFolderPath().toString(), "pipelines");
    }

    private boolean configExists() {
        return getConfigFolderExists() && Files.exists(getConfigPath());
    }

    private boolean driverModeExists() {
        return getConfigFolderExists() && Files.exists(getDriverModePath());
    }
}

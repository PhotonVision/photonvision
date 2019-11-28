package com.chameleonvision.config;

import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.CVPipeline2dSettings;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraConfig {

    private static final Path camerasConfigFolderPath = Paths.get(ConfigManager.SettingsPath.toString(), "cameras");

    private final String cameraConfigName;
    private final CameraJsonConfig preliminaryConfig;

    private final PipelineConfig pipelineConfig;

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

        return new FullCameraConfiguration(loadConfig(), pipelineConfig.load(), loadDriverMode());
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

    List<CVPipelineSettings> loadPipelines() {
        List<CVPipelineSettings> pipelines = new ArrayList<>();
        try {
            var pipelineArray = JacksonHelper.deserializer(getPipelinesPath(), CVPipelineSettings[].class);
//            pipelines = Arrays.asList(pipelineArray);
        } catch (Exception e) {
            System.err.println("Failed to load camera pipelines: " + getPipelinesPath().toString());
        }
        return pipelines;
    }

    CVPipelineSettings loadDriverMode() {
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
        try {
            JacksonHelper.serializer(getPipelinesPath(), pipelines.toArray());
        } catch (IOException e) {
            System.err.println("Failed to save camera pipelines file: " + getConfigPath().toString());
        }
    }

    void saveDriverMode(CVPipelineSettings driverMode) {
        try {
            JacksonHelper.serializer(getDriverModePath(), driverMode);
        } catch (IOException e) {
            System.err.println("Failed to save camera drivermode file: " + getDriverModePath().toString());
        }
    }

    private void checkFolder() {
        if (!folderExists()) {
            try {
                if (!(new File(getFolderPath().toUri()).mkdirs())) {
                    System.err.println("Failed to create camera config folder: " + getFolderPath().toString());
                }
            } catch(Exception e) {
                System.err.println("Failed to create camera config folder: " + getFolderPath().toString());
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

    private Path getFolderPath() {
        return Paths.get(camerasConfigFolderPath.toString(), cameraConfigName);
    }

    private Path getConfigPath() {
        return Paths.get(getFolderPath().toString(), "camera.json");
    }

    Path getPipelinesPath() {
        return Paths.get(getFolderPath().toString(), "pipelines.json");
    }

    private Path getDriverModePath() {
        return Paths.get(getFolderPath().toString(), "drivermode.json");
    }

    boolean folderExists() {
        return Files.exists(getFolderPath());
    }

    private boolean configExists() {
        return folderExists() && Files.exists(getConfigPath());
    }

    private boolean driverModeExists() {
        return folderExists() && Files.exists(getDriverModePath());
    }

    boolean pipelinesExists() {
        return folderExists() && Files.exists(getPipelinesPath());
    }
}

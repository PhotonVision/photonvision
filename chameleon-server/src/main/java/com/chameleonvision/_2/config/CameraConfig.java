package com.chameleonvision._2.config;

import com.chameleonvision._2.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.common.util.file.FileUtils;
import com.chameleonvision.common.util.jackson.JacksonUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CameraConfig {

    private static final Path camerasConfigFolderPath =
            Path.of(ConfigManager.SettingsPath.toString(), "cameras");

    private final CameraJsonConfig preliminaryConfig;
    private final Path configFolderPath;
    private final Path configPath;
    private final Path driverModePath;
    private final Path calibrationPath;
    final Path pipelineFolderPath;

    public final PipelineConfig pipelineConfig;

    CameraConfig(CameraJsonConfig config) {
        preliminaryConfig = config;
        String cameraConfigName = preliminaryConfig.name.replace(' ', '_');
        pipelineConfig = new PipelineConfig(this);

        configFolderPath = Path.of(camerasConfigFolderPath.toString(), cameraConfigName);
        configPath = Path.of(configFolderPath.toString(), "camera.json");
        driverModePath = Path.of(configFolderPath.toString(), "drivermode.json");
        calibrationPath = Path.of(configFolderPath.toString(), "calibration.json");
        pipelineFolderPath = Paths.get(configFolderPath.toString(), "pipelines");
    }

    public FullCameraConfiguration load() {
        checkFolder();
        checkConfig();
        checkDriverMode();
        checkCalibration();
        pipelineConfig.check();

        return new FullCameraConfiguration(
                loadConfig(), pipelineConfig.load(), loadDriverMode(), loadCalibration(), this);
    }

    private CameraJsonConfig loadConfig() {
        CameraJsonConfig config = preliminaryConfig;
        try {
            config = JacksonUtils.deserialize(configPath, CameraJsonConfig.class);
        } catch (IOException e) {
            System.err.printf(
                    "Failed to load camera config: %s - using default.\n", configPath.toString());
        }
        return config;
    }

    private CVPipelineSettings loadDriverMode() {
        CVPipelineSettings driverMode = new CVPipelineSettings();
        try {
            driverMode = JacksonUtils.deserialize(driverModePath, CVPipelineSettings.class);
        } catch (IOException e) {
            System.err.println("Failed to load camera drivermode: " + driverModePath.toString());
        }
        if (driverMode != null) {
            driverMode.nickname = "DRIVERMODE";
            driverMode.index = -1;
        }
        return driverMode;
    }

    private List<CameraCalibrationConfig> loadCalibration() {
        List<CameraCalibrationConfig> calibrations = new ArrayList<>();
        try {
            calibrations =
                    List.of(
                            Objects.requireNonNull(
                                    JacksonUtils.deserialize(calibrationPath, CameraCalibrationConfig[].class)));
        } catch (Exception e) {
            System.err.println("Failed to load camera calibration: " + driverModePath.toString());
        }
        return calibrations;
    }

    void saveConfig(CameraJsonConfig config) {
        try {
            JacksonUtils.serializer(configPath, config, true);
            FileUtils.setFilePerms(configPath);
        } catch (IOException e) {
            System.err.println("Failed to save camera config file: " + configPath.toString());
        }
    }

    void savePipelines(List<CVPipelineSettings> pipelines) {
        pipelineConfig.save(pipelines);
    }

    public void saveDriverMode(CVPipelineSettings driverMode) {
        try {
            JacksonUtils.serializer(driverModePath, driverMode, true);
            FileUtils.setFilePerms(driverModePath);
        } catch (IOException e) {
            System.err.println("Failed to save camera drivermode file: " + driverModePath.toString());
        }
    }

    public void saveCalibration(List<CameraCalibrationConfig> cal) {
        CameraCalibrationConfig[] configs = cal.toArray(new CameraCalibrationConfig[0]);
        try {
            JacksonUtils.serializer(calibrationPath, configs, true);
            FileUtils.setFilePerms(calibrationPath);
        } catch (IOException e) {
            System.err.println("Failed to save camera calibration file: " + calibrationPath.toString());
        }
    }

    void checkFolder() {
        if (!configFolderExists()) {
            try {
                if (!(new File(configFolderPath.toUri()).mkdirs())) {
                    System.err.println(
                            "Failed to create camera config folder: " + configFolderPath.toString());
                }
                FileUtils.setFilePerms(configFolderPath);
            } catch (Exception e) {
                System.err.println("Failed to create camera config folder: " + configFolderPath.toString());
            }
        }
    }

    private void checkConfig() {
        if (!configExists()) {
            try {
                JacksonUtils.serializer(configPath, preliminaryConfig, true);
                FileUtils.setFilePerms(configPath);
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
                JacksonUtils.serializer(driverModePath, newDriverModeSettings, true);
                FileUtils.setFilePerms(driverModePath);
            } catch (IOException e) {
                System.err.println("Failed to create camera drivermode file: " + driverModePath.toString());
            }
        }
    }

    private void checkCalibration() {
        if (!calibrationExists()) {
            try {
                List<CameraCalibrationConfig> calibrations = new ArrayList<>();
                JacksonUtils.serializer(calibrationPath, calibrations.toArray(), true);
            } catch (IOException e) {
                System.err.println(
                        "Failed to create camera calibration file: " + calibrationPath.toString());
            }
        }
    }

    private boolean configFolderExists() {
        return Files.exists(configFolderPath);
    }

    private boolean configExists() {
        return configFolderExists() && Files.exists(configPath);
    }

    private boolean driverModeExists() {
        return configFolderExists() && Files.exists(driverModePath);
    }

    private boolean calibrationExists() {
        return configFolderExists() && Files.exists(calibrationPath);
    }
}

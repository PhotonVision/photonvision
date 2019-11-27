package com.chameleonvision.config;

import com.chameleonvision.util.ProgramDirectoryUtilities;
import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ConfigManager {
    private ConfigManager() {}

    static final Path SettingsPath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "settings");
    private static final Path settingsFilePath = Paths.get(SettingsPath.toString(), "settings.json");

    private static final LinkedHashMap<String, CameraConfig> cameraConfigs = new LinkedHashMap<>();

    public static GeneralSettings settings = new GeneralSettings();

    private static boolean settingsFolderExists() { return Files.exists(SettingsPath); }
    private static boolean settingsFileExists() { return settingsFolderExists() && Files.exists(settingsFilePath); }

    private static void checkSettingsFolder() {
        if (!settingsFolderExists()) {
            try {
                Files.createDirectory(SettingsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void checkSettingsFile() {
        boolean settingsFileEmpty = settingsFileExists() && new File(settingsFilePath.toString()).length() == 0;
        if (settingsFileEmpty || !settingsFileExists()) {
            try {
                JacksonHelper.serializer(settingsFilePath, settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                settings = JacksonHelper.deserializer(settingsFilePath, GeneralSettings.class);
            } catch (IOException e) {
                System.err.println("Failed to load settings.json, using defaults.");
            }
        }
    }

    public static void initializeSettings() {
        System.out.println("Settings folder: " + SettingsPath.toString());
        checkSettingsFolder();
        checkSettingsFile();
    }

    private static void saveSettingsFile() {
        try {
            JacksonHelper.serializer(settingsFilePath, settings);
        } catch (IOException e) {
            System.err.println("Failed to save settings.json!");
        }
    }

    public static void saveGeneralSettings() {
        checkSettingsFolder();
        saveSettingsFile();
    }

    // TODO: (HIGH) cleanup!
    public static List<FullCameraConfiguration> initializeCameras(List<CameraJsonConfig> preliminaryConfigs) {
        List<FullCameraConfiguration> configList = new ArrayList<>();

        checkSettingsFolder();

        // loop over all the camera names and try to create settings folders for it
        for (CameraJsonConfig preliminaryConfig : preliminaryConfigs) {
            CameraConfig cameraConfiguration = new CameraConfig(preliminaryConfig);
            cameraConfigs.put(preliminaryConfig.name, cameraConfiguration);

            CameraJsonConfig camJsonConfig = cameraConfiguration.load();
            List<CVPipelineSettings> pipelines = cameraConfiguration.loadPipelines();
            CVPipelineSettings driverMode = cameraConfiguration.loadDriverMode();

            configList.add(new FullCameraConfiguration(camJsonConfig, pipelines, driverMode));
        }

        return configList;
    }

    public static void saveCameraConfig(String cameraName, CameraJsonConfig config) {
        cameraConfigs.get(cameraName).saveConfig(config);
    }

    public static void saveCameraPipelines(String cameraName, List<CVPipelineSettings> pipelines) {
        cameraConfigs.get(cameraName).savePipelines(pipelines);
    }

    public static void saveCameraDriverMode(String cameraName, CVPipelineSettings driverMode) {
        cameraConfigs.get(cameraName).saveDriverMode(driverMode);
    }
}

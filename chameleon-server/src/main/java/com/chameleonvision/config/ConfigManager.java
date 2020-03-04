package com.chameleonvision.config;

import com.chameleonvision.util.*;
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
    private ConfigManager() {
    }

    public static final Path SettingsPath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "settings");
    private static final Path settingsFilePath = Paths.get(SettingsPath.toString(), "settings.json");

    private static final LinkedHashMap<String, CameraConfig> cameraConfigs = new LinkedHashMap<>();

    public static GeneralSettings settings = new GeneralSettings();

    private static boolean settingsFolderExists() {
        return Files.exists(SettingsPath);
    }

    private static boolean settingsFileExists() {
        return settingsFolderExists() && Files.exists(settingsFilePath);
    }

    private static void checkSettingsFolder() {
        if (!settingsFolderExists()) {
            try {
                if (!(new File(SettingsPath.toUri()).mkdirs())) {
                    System.err.println("Failed to create settings folder: " + SettingsPath.toString());
                }
                Files.createDirectory(SettingsPath);
                if (!Platform.CurrentPlatform.isWindows()) {
                    new ShellExec().executeBashCommand("sudo chmod -R 0777 " + SettingsPath.toString());
                }
            } catch (IOException e) {
                if (!(e instanceof java.nio.file.FileAlreadyExistsException))
                    e.printStackTrace();
            }
        }
    }

    private static void checkSettingsFile() {
        boolean settingsFileEmpty = settingsFileExists() && new File(settingsFilePath.toString()).length() == 0;
        if (settingsFileEmpty || !settingsFileExists()) {
            try {
                JacksonHelper.serializer(settingsFilePath, settings, true);
                FileHelper.setFilePerms(settingsFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                settings = JacksonHelper.deserialize(settingsFilePath, GeneralSettings.class);
            } catch (IOException e) {
                System.err.println("Failed to load settings.json, using defaults.");
            }
        }
    }

    public static void initializeSettings() {
        System.out.println("Settings folder: " + SettingsPath.toString());
        checkSettingsFolder();
        checkSettingsFile();
        FileHelper.setAllPerms(SettingsPath);
    }

    private static void saveSettingsFile() {
        try {
            JacksonHelper.serializer(settingsFilePath, settings, true);
            FileHelper.setFilePerms(settingsFilePath);
        } catch (IOException e) {
            System.err.println("Failed to save settings.json!");
        }
    }

    public static void saveGeneralSettings() {
        checkSettingsFolder();
        saveSettingsFile();
    }

    public static List<FullCameraConfiguration> initializeCameras(List<CameraJsonConfig> preliminaryConfigs) {
        List<FullCameraConfiguration> configList = new ArrayList<>();

        checkSettingsFolder();

        // loop over all the camera names and try to create settings folders for it
        for (CameraJsonConfig preliminaryConfig : preliminaryConfigs) {
            CameraConfig cameraConfiguration = new CameraConfig(preliminaryConfig);
            cameraConfigs.put(preliminaryConfig.name, cameraConfiguration);

            FullCameraConfiguration camJsonConfig = cameraConfiguration.load();

            configList.add(camJsonConfig);
        }

        return configList;
    }

    public static void saveCameraConfig(String cameraName, CameraJsonConfig config) {
        var camConf = cameraConfigs.get(cameraName);
        camConf.saveConfig(config);
    }

    public static void saveCameraPipelines(String cameraName, List<CVPipelineSettings> pipelines) {
        var camConf = cameraConfigs.get(cameraName);
        camConf.savePipelines(pipelines);
    }

    public static void saveCameraDriverMode(String cameraName, CVPipelineSettings driverMode) {
        var camConf = cameraConfigs.get(cameraName);
        camConf.saveDriverMode(driverMode);
    }
}

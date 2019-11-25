package com.chameleonvision.config;

import com.chameleonvision.util.ProgramDirectoryUtilities;
import com.chameleonvision.util.FileHelper;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private ConfigManager() {}

    private static final Path SettingsPath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "settings");
    private static final Path cameraConfigPath = Paths.get(SettingsPath.toString(), "cameras");
    private static final Path settingsFilePath = Paths.get(SettingsPath.toString(), "settings.json");

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
                FileHelper.Serializer(settingsFilePath, settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                settings = FileHelper.DeSerializer(settingsFilePath, GeneralSettings.class);
            } catch (IOException e) {
                System.err.println("Failed to load settings.json, using defaults.");
            }
        }
    }

    public static void initializeSettings() {
        checkSettingsFolder();
        checkSettingsFile();
    }

    private static void saveSettingsFile() {
        try {
            FileHelper.Serializer(settingsFilePath, settings);
        } catch (IOException e) {
            System.err.println("Failed to save settings.json!");
        }
    }

    public static void saveSettings() {
        checkSettingsFolder();
        saveSettingsFile();
    }

    private static Path getCameraSpecificFolderPath(String cameraName) {
        return Paths.get(cameraConfigPath.toString(), cameraName);
    }

    private static Path getCameraSpecificConfigPath(String cameraName) {
        return Paths.get(getCameraSpecificFolderPath(cameraName).toString(), "camera.json");
    }

    private static Path getCameraSpecificPipelinesPath(String cameraName) {
        return Paths.get(getCameraSpecificFolderPath(cameraName).toString(), "pipelines.json");
    }

    private static Path getCameraSpecificDriverModePath(String cameraName) {
        return Paths.get(getCameraSpecificFolderPath(cameraName).toString(), "drivermode.json");
    }

    private static boolean cameraFolderExists(String cameraName) {
        return Files.exists(getCameraSpecificFolderPath(cameraName));
    }

    private static boolean cameraConfigExists(String cameraName) {
        return cameraFolderExists(cameraName) && Files.exists(getCameraSpecificConfigPath(cameraName));
    }

    private static boolean cameraPipelinesExists(String cameraName) {
        return cameraFolderExists(cameraName) && Files.exists(getCameraSpecificPipelinesPath(cameraName));
    }

    private static boolean cameraDriverModeExists(String cameraName) {
        return cameraFolderExists(cameraName) && Files.exists(getCameraSpecificDriverModePath(cameraName));
    }

    // TODO: (HIGH) cleanup!
    public static List<CameraConfig> initializeCameraConfig(List<CameraConfig> preliminaryConfigs) {
        var configList = new ArrayList<CameraConfig>();

        checkSettingsFolder();

        // loop over all the camera names and try to create settings folders for it
        preliminaryConfigs.forEach((preliminaryConfig) -> {
            String cameraName = preliminaryConfig.name;

            final Path cameraConfigFolderPath = getCameraSpecificFolderPath(cameraName);
            final Path cameraConfigPath = getCameraSpecificConfigPath(cameraName);

            // check if the config folder exists, and if not, create it
            if (!cameraFolderExists(cameraName)) {
                try {
                    Files.createDirectory(cameraConfigFolderPath);
                } catch (IOException e) {
                    System.err.println("Failed to create camera config folder!");
                }
            } else {
                CameraConfig config = preliminaryConfig;

                // check if the config exists, and if not, create it
                if(!cameraConfigExists(cameraName)) {
                    try {
                        FileHelper.Serializer(cameraConfigPath, preliminaryConfig);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        config = FileHelper.DeSerializer(cameraConfigPath, CameraConfig.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                configList.add(config);
            }
        });

        return configList;
    }
    public static void saveCameraPipelines(String cameraName, List<CVPipelineSettings> pipelines) throws IOException {
        Path cameraFolder = Paths.get(cameraConfigPath.toString(), cameraName);
        Path filePath = Paths.get(cameraFolder.toString(), cameraName,"pipelines.json");
        FileHelper.CheckPath(cameraFolder);
        FileHelper.Serializer(filePath, pipelines);
    }
    public static void saveCameraDriverMode(String cameraName, CVPipelineSettings driverMode) throws IOException {
        Path cameraFolder = Paths.get(cameraConfigPath.toString(), cameraName);
        Path filePath = Paths.get(cameraFolder.toString(), cameraName,"driverMode.json");
        FileHelper.CheckPath(cameraFolder);
        FileHelper.Serializer(filePath, driverMode);
    }
    public static void saveCameraConfig(String cameraName, CameraConfig config) throws IOException {
        Path cameraFolder = Paths.get(cameraConfigPath.toString(), cameraName);
        Path filePath = Paths.get(cameraFolder.toString(), cameraName,"driverMode.json");
        FileHelper.CheckPath(cameraFolder);
        FileHelper.Serializer(filePath, config);
    }
}

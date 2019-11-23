package com.chameleonvision.config;

import com.chameleonvision.util.ProgramDirectoryUtilities;
import com.chameleonvision.util.FileHelper;

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
        if (!settingsFileExists()) {
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

        // loop over all the camera names and try to create settings folders for it
        preliminaryConfigs.forEach((preliminaryConfig) -> {

            final Path cameraConfigFolderPath = Paths.get(cameraConfigPath.toString(), String.format("%s\\", preliminaryConfig.name));
            final Path cameraConfigPath = Paths.get(cameraConfigFolderPath.toString(), "camera.json");

            // check if the config folder exists, and if not, create it
            if (Files.notExists(cameraConfigFolderPath)) {
                try {
                    Files.createDirectory(cameraConfigFolderPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                CameraConfig config = preliminaryConfig;
                if(!Files.exists(cameraConfigPath)) {
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
}

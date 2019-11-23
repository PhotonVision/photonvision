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

    public static GeneralSettings settings = new GeneralSettings();

    public static void initializeSettings() {
        if (Files.notExists(SettingsPath)) {
            try {
                Files.createDirectory(SettingsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // try to load the settings file and deserialize it
        Path settingsFilePath = Paths.get(SettingsPath.toString(), "settings.json");

        if (Files.notExists(settingsFilePath)) {
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

package com.chameleonvision.classabstraction.config;

import com.chameleonvision.classabstraction.util.ProgramDirectoryUtilities;
import com.chameleonvision.settings.GeneralSettings;
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
    private static final Path CamConfigPath = Paths.get(SettingsPath.toString(), "cameras");

    public static GeneralSettings settings = new GeneralSettings();

    public static void initializeSettings() {
        boolean settingsFolderExists = Files.exists(SettingsPath);

        if (!settingsFolderExists) {
            try {
                Files.createDirectory(SettingsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Path settingsFilePath = Paths.get(SettingsPath.toString(), "settings.json");
        boolean settingsFileExists = Files.exists(settingsFilePath);

        if (!settingsFileExists) {
            try {
                FileHelper.Serializer(settings, settingsFilePath);
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

    public static List<CameraConfig> initializeCameraConfig(List<String> cameraNames) {

        cameraNames.forEach((cameraName) -> {
            final Path cameraConfigFolder = Paths.get(CamConfigPath.toString(), String.format("%s\\", cameraName));
            final Path cameraConfigPath = Paths.get(cameraConfigFolder.toString(), String.format("%s.json", cameraName));
            final Path cameraPipelinesPath = Paths.get(cameraConfigFolder.toString(), "pipelines.json");
            final Path cameraDrivermodePath = Paths.get(cameraConfigFolder.toString(), "drivermode.json");

            boolean cameraConfigFolderExists = Files.exists(cameraConfigFolder);

            if (!cameraConfigFolderExists) {
                try {
                    Files.createDirectory(cameraConfigFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        return new ArrayList<>();
    }
}

package com.chameleonvision.classabstraction.config;

import com.chameleonvision.classabstraction.util.ProgramDirectoryUtilities;
import com.chameleonvision.settings.GeneralSettings;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.util.FileHelper;
import com.chameleonvision.vision.camera.CameraDeserializer;
import com.chameleonvision.vision.camera.USBCamera;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigManager {
    private ConfigManager() {}

    private static final Path SettingsPath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "settings");
    private static final Path cameraConfigPath = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "cameras");

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

        // try to load the settings file and deserialize it
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

        // try to load camera file and deserialize it
//        var cameraConfigs = initializeCameraConfig();

    }

    public static List<CameraConfig> initializeCameraConfig(List<String> cameraNames) {

        var configList = new ArrayList<CameraConfig>();


        // loop over all the camera names and try to create settings folders for it
        cameraNames.forEach((cameraName) -> {
            final Path cameraConfigFolder = Paths.get(cameraConfigPath.toString(), String.format("%s\\", cameraName));
            final Path cameraConfigPath = Paths.get(cameraConfigFolder.toString(), String.format("%s.json", cameraName));

            // check if the config folder exists, and if not, create it
            boolean cameraConfigFolderExists = Files.exists(cameraConfigFolder);

            if (!cameraConfigFolderExists) {
                try {
                    Files.createDirectory(cameraConfigFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // try to deserialize the file
            var camJsonFile = new File(cameraConfigPath.toString());
            if(camJsonFile.exists() && camJsonFile.length() > 0) {
                try {
                    var config = FileHelper.DeSerializer(cameraConfigPath, CameraConfig.class);
                    configList.add(config);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // create a default one
                configList.add(new CameraConfig(
                        70.0,
                        cameraConfigPath.toString(),
                        cameraName,
                        cameraName
                ));
            }


        });

        return configList;
    }
}

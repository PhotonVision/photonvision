package com.chameleonvision.settings;

import com.chameleonvision.FileHelper;
import com.chameleonvision.vision.GeneralSettings;
import com.chameleonvision.vision.camera.CameraManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager {
    private static SettingsManager instance;

    private SettingsManager() {
        InitiateGeneralSettings();

        var allCameras = CameraManager.getAllCamerasByName();
        if (!allCameras.containsKey(GeneralSettings.curr_camera) && allCameras.size() > 0) {
            var cam = allCameras.entrySet().stream().findFirst().get().getValue();
            GeneralSettings.curr_camera = cam.name;
            GeneralSettings.curr_pipeline = cam.getCurrentPipelineIndex();
        }
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            synchronized (SettingsManager.class) {
                if (instance == null) {
                    instance = new SettingsManager();
                }
            }
        }
        return instance;
    }

    public static com.chameleonvision.vision.GeneralSettings GeneralSettings;
    //    public static HashMap<String, String> CameraPorts = new HashMap<>();//TODO Implement ports
    public static final Path SettingsPath = Paths.get(System.getProperty("user.dir"), "Settings");


    private void InitiateGeneralSettings() {
        FileHelper.CheckPath(SettingsPath);
        try {
            GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(), "Settings.json").toString()), com.chameleonvision.vision.GeneralSettings.class);
        } catch (FileNotFoundException e) {
            GeneralSettings = new GeneralSettings();
        }
    }

    //Access Methods
    public void updateCameraSetting(String cameraName, int pipelineNumber) {
        GeneralSettings.curr_camera = cameraName;
        GeneralSettings.curr_pipeline = pipelineNumber;
    }

    public void updatePipelineSetting(int pipelineNumber){
        GeneralSettings.curr_pipeline = pipelineNumber;
    }

    //Savers
    public void SaveSettings() {
        CameraManager.saveCameras();
        SaveGeneralSettings();
    }

    private void SaveGeneralSettings() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(Paths.get(SettingsPath.toString(), "settings.json").toString());
            gson.toJson(GeneralSettings, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

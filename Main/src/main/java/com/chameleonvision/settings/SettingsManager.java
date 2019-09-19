package com.chameleonvision.settings;

import com.chameleonvision.FileHelper;
import com.chameleonvision.CameraException;

import java.io.*;
import java.nio.file.*;

import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.GeneralSettings;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.CameraManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.wpi.cscore.*;

public class SettingsManager {
    private static SettingsManager instance;

    private SettingsManager() {
        InitiateGeneralSettings();

        if (!Cameras.containsKey(GeneralSettings.curr_camera) && Cameras.size() > 0) {
            String camName = Cameras.keySet().stream().findFirst().get();
            GeneralSettings.curr_camera = camName;
            GeneralSettings.curr_pipeline = CameraManager.getCamera(camName).getCurrentPipelineIndex();
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

    public static HashMap<String, Camera> Cameras = new HashMap<>();
    public static HashMap<String, UsbCamera> UsbCameras = new HashMap<>();
    public static com.chameleonvision.vision.GeneralSettings GeneralSettings;
    public static HashMap<String, String> CamerasCurrentPipeline = new HashMap<>();
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
    public List<String> GetResolutionList() throws CameraException {
        if (!GeneralSettings.curr_camera.equals("")) {
            List<String> list = new ArrayList<>();
            var cam = CameraManager.getCamera(GeneralSettings.curr_camera).UsbCam;
            for (var res : cam.enumerateVideoModes()) {
                list.add(String.format("%s X %s at %s fps", res.width, res.height, res.fps));
            }
            return list;
        }
        throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
    }

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

    private void SaveCameras() {
        for (Map.Entry<String, Camera> entry : Cameras.entrySet()) {
            try {
                Gson gson = new Gson();
                FileWriter writer = new FileWriter(Paths.get(CameraManager.CamConfigPath.toString(), String.format("%s.json", entry.getKey())).toString());
                gson.toJson(entry.getValue(), writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void SaveGeneralSettings() {
        try {
            FileWriter writer = new FileWriter(Paths.get(SettingsPath.toString(), "Settings.json").toString());
            new Gson().toJson(GeneralSettings, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
